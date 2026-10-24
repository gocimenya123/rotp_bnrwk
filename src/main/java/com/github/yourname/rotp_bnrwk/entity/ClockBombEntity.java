package com.github.yourname.rotp_bnrwk.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.yourname.rotp_bnrwk.init.InitEntities;
import com.github.yourname.rotp_bnrwk.init.InitSounds;
import com.github.yourname.rotp_bnrwk.network.UseKeyTracker;
import com.github.yourname.rotp_bnrwk.util.BombUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * A small clock-shaped pin ("contact bomb").
 *
 * Life cycle:  FLYING (thrown) -> STUCK (block / fluid / mid-air in smoke) or ATTACHED (follows an entity or item)
 * -> the pin springs up during the last {@link #SPRING_TICKS} ticks -> explodes.
 *
 * The fuse is {@code FUSE_TOTAL - tickCount}, so it needs no per-tick network sync.
 */
public class ClockBombEntity extends Entity {
    /** Ticks before the explosion during which the pin visibly springs up. */
    public static final int SPRING_TICKS = 12;

    public static final byte STATE_FLYING = 0;
    public static final byte STATE_STUCK = 1;
    public static final byte STATE_ATTACHED = 2;

    private static final DataParameter<Integer> FUSE_TOTAL = EntityDataManager.defineId(ClockBombEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.defineId(ClockBombEntity.class, DataSerializers.INT);
    private static final DataParameter<Byte> STATE = EntityDataManager.defineId(ClockBombEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Direction> FACING = EntityDataManager.defineId(ClockBombEntity.class, DataSerializers.DIRECTION);
    private static final DataParameter<Boolean> SMALL = EntityDataManager.defineId(ClockBombEntity.class, DataSerializers.BOOLEAN);
    /** True while some player is holding the fuse back (client uses it for particles). */
    private static final DataParameter<Boolean> HELD = EntityDataManager.defineId(ClockBombEntity.class, DataSerializers.BOOLEAN);

    /** A holder further away than this lets go of the pin. */
    public static final double HOLD_RANGE = 12.0D;

    private UUID ownerUUID;
    private UUID targetUUID;
    private UUID holderUUID;
    private int holdGrace;
    private Vector3d attachOffset = Vector3d.ZERO;
    private float damage = 18.0F;
    private float radius = 3.0F;

    public ClockBombEntity(EntityType<? extends ClockBombEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
    }

    public ClockBombEntity(World world) {
        this(InitEntities.CLOCK_BOMB.get(), world);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FUSE_TOTAL, 60);
        entityData.define(TARGET_ID, -1);
        entityData.define(STATE, STATE_FLYING);
        entityData.define(FACING, Direction.UP);
        entityData.define(SMALL, false);
        entityData.define(HELD, false);
    }

    // ------------------------------------------------------------------------------------------------ setup API

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (ownerUUID != null && level instanceof ServerWorld) {
            Entity e = ((ServerWorld) level).getEntity(ownerUUID);
            if (e instanceof LivingEntity) {
                return (LivingEntity) e;
            }
        }
        return null;
    }

    public boolean isOwnedBy(@Nullable Entity entity) {
        return entity != null && ownerUUID != null && ownerUUID.equals(entity.getUUID());
    }

    /** damage / radius of the explosion, fuse length in ticks, and whether this is a tiny smoke-cloud pin. */
    public void configure(float damage, float radius, int fuseTicks, boolean small) {
        this.damage = damage;
        this.radius = radius;
        entityData.set(FUSE_TOTAL, fuseTicks);
        entityData.set(SMALL, small);
    }

    /** Throw the pin; it will stick to the first thing it hits. */
    public void launch(Vector3d velocity) {
        entityData.set(STATE, STATE_FLYING);
        setDeltaMovement(velocity);
    }

    /** Fix the pin in place (block surface, water surface, or floating in the air). */
    public void stickAt(Vector3d point, Direction face) {
        entityData.set(STATE, STATE_STUCK);
        entityData.set(FACING, face);
        entityData.set(TARGET_ID, -1);
        targetUUID = null;
        setDeltaMovement(Vector3d.ZERO);
        setCenter(point);
    }

    /** Plant the pin onto an entity (mob, player, item, boat...). It follows the target until it explodes. */
    public void attachTo(Entity target, Vector3d hitPoint, Direction face) {
        attachTo(target, hitPoint, face, true);
    }

    /**
     * @param scaleToSize if true the explosion grows with the size of the target (see {@link #sizeFactor}); pass false
     *                    when re-attaching a pin whose damage / radius were already scaled.
     */
    public void attachTo(Entity target, Vector3d hitPoint, Direction face, boolean scaleToSize) {
        entityData.set(STATE, STATE_ATTACHED);
        entityData.set(FACING, face);
        entityData.set(TARGET_ID, target.getId());
        targetUUID = target.getUUID();
        attachOffset = hitPoint.subtract(target.position());
        setDeltaMovement(Vector3d.ZERO);
        setCenter(hitPoint);
        if (scaleToSize && !isSmall()) {
            float f = sizeFactor(target);
            damage *= f;
            radius = Math.min(radius * f, MAX_RADIUS);
        }
    }

    /** Max explosion radius after size scaling. */
    public static final float MAX_RADIUS = 8.0F;

    /**
     * Bigger target - bigger and stronger blast. 1.0 is a player-sized body (0.6 x 0.6 x 1.8); the factor follows the
     * cube root of the bounding-box volume, clamped to 0.7 .. 2.5 (an iron golem is ~2x, a dragon hits the cap).
     */
    public static float sizeFactor(Entity target) {
        if (target instanceof ItemEntity) {
            return 1.0F; // dropped items keep the ability's base power
        }
        double volume = target.getBbWidth() * target.getBbWidth() * target.getBbHeight();
        return (float) MathHelper.clamp(Math.cbrt(volume / 0.648D), 0.7D, 2.5D);
    }

    // ------------------------------------------------------------------------------------------------ accessors

    public byte getState() { return entityData.get(STATE); }
    public Direction getFacing() { return entityData.get(FACING); }
    public boolean isSmall() { return entityData.get(SMALL); }
    public boolean isHeld() { return entityData.get(HELD); }
    public float getDamage() { return damage; }
    public float getRadius() { return radius; }
    public int getFuseLeft() { return entityData.get(FUSE_TOTAL) - tickCount; }

    /** 0 = pin hidden, 1 = fully sprung up. Used by the renderer. */
    public float getSpringProgress(float partialTick) {
        float left = getFuseLeft() - partialTick;
        return MathHelper.clamp(1.0F - left / SPRING_TICKS, 0.0F, 1.0F);
    }

    private void setCenter(Vector3d c) {
        setPos(c.x, c.y - getBbHeight() * 0.5D, c.z);
    }

    private Vector3d getCenter() {
        return position().add(0, getBbHeight() * 0.5D, 0);
    }

    // ------------------------------------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();

        switch (getState()) {
        case STATE_FLYING:
            tickFlying();
            break;
        case STATE_ATTACHED:
            tickAttached();
            break;
        default:
            break;
        }

        // Held by a player: the fuse is pushed back every tick, so the pin can't go off.
        if (!level.isClientSide()) {
            updateHold();
        }

        int left = getFuseLeft();
        if (level.isClientSide()) {
            if (isHeld() && tickCount % 10 == 0) {
                Vector3d c = getCenter();
                level.addParticle(ParticleTypes.ENCHANT, c.x, c.y + 0.2D, c.z, 0, 0.1D, 0);
            }
            if (!isHeld() && left <= SPRING_TICKS && left > 0 && tickCount % 2 == 0) {
                Vector3d c = getCenter();
                level.addParticle(ParticleTypes.CRIT, c.x, c.y, c.z, 0, 0.12D, 0);
            }
            return;
        }

        if (isHeld()) {
            return; // fuse frozen, no ticking sounds, no explosion
        }
        if (left == SPRING_TICKS) {
            playSound(InitSounds.PIN_SPRING.get(), isSmall() ? 0.5F : 1.0F, 1.0F);
        } else if (left > SPRING_TICKS) {
            // the rhythm speeds up: every second, then every half second, then every quarter second
            int interval = isSmall() || left > 60 ? 20 : left > 30 ? 10 : 5;
            if (left % interval == 0) {
                boolean tick = (left / interval) % 2 == 0;
                float pitch = 1.0F + (left <= 30 ? 0.08F : 0.0F);
                playSound(tick ? InitSounds.TICK.get() : InitSounds.TOCK.get(), isSmall() ? 0.35F : 0.8F, pitch);
            }
        }
        if (left <= 0 || getY() < -64) {
            explode();
        }
    }

    private void tickFlying() {
        if (!level.isClientSide()) {
            if (isInWaterOrBubble() || isInLava()) {
                stickAt(getCenter(), Direction.UP);
                return;
            }
            RayTraceResult hit = ProjectileHelper.getHitResult(this, this::canHitEntity);
            if (hit.getType() == RayTraceResult.Type.BLOCK) {
                BlockRayTraceResult block = (BlockRayTraceResult) hit;
                Direction face = block.getDirection();
                stickAt(block.getLocation().add(face.getStepX() * 0.04D, face.getStepY() * 0.04D, face.getStepZ() * 0.04D), face);
                return;
            } else if (hit.getType() == RayTraceResult.Type.ENTITY) {
                Entity target = ((EntityRayTraceResult) hit).getEntity();
                attachTo(target, hit.getLocation(), BombUtil.faceFromEntityHit(target, hit.getLocation()));
                return;
            }
        }
        Vector3d motion = getDeltaMovement().scale(0.99D).add(0, -0.04D, 0);
        setDeltaMovement(motion);
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    private void tickAttached() {
        Entity target = level.getEntity(entityData.get(TARGET_ID));
        if (target == null && !level.isClientSide() && targetUUID != null && level instanceof ServerWorld) {
            target = ((ServerWorld) level).getEntity(targetUUID);
            if (target != null) {
                entityData.set(TARGET_ID, target.getId());
            }
        }
        if (level.isClientSide()) {
            return; // the server moves us, the client just interpolates
        }
        if (target == null || !target.isAlive()) {
            // The carrier is gone (killed, picked up...) - the pin stays where it was and keeps ticking.
            stickAt(getCenter(), getFacing());
            return;
        }
        setCenter(target.position().add(attachOffset));
    }

    // ------------------------------------------------------------------------------------------------ holding

    @Nullable
    private PlayerEntity getHolder() {
        if (holderUUID != null && level instanceof ServerWorld) {
            Entity e = ((ServerWorld) level).getEntity(holderUUID);
            if (e instanceof PlayerEntity) {
                return (PlayerEntity) e;
            }
        }
        return null;
    }

    /**
     * A pin stuck on a player can be held back by that player: both hands empty and the right mouse button pressed.
     */
    private boolean isHeldByCarrier() {
        if (getState() != STATE_ATTACHED || targetUUID == null || !(level instanceof ServerWorld)) {
            return false;
        }
        Entity target = ((ServerWorld) level).getEntity(targetUUID);
        if (!(target instanceof PlayerEntity) || !target.isAlive()) {
            return false;
        }
        PlayerEntity carrier = (PlayerEntity) target;
        return carrier.getMainHandItem().isEmpty() && carrier.getOffhandItem().isEmpty() && UseKeyTracker.isDown(carrier);
    }

    private void updateHold() {
        if (isHeldByCarrier()) {
            entityData.set(FUSE_TOTAL, entityData.get(FUSE_TOTAL) + 1);
            entityData.set(HELD, true);
            return;
        }
        if (holderUUID == null) {
            entityData.set(HELD, false);
            return;
        }
        PlayerEntity holder = getHolder();
        if (holdGrace > 0) {
            holdGrace--;
        }
        boolean valid = holder != null && holder.isAlive() && !holder.isSpectator()
                && holder.distanceToSqr(this) <= HOLD_RANGE * HOLD_RANGE;
        if (!valid || (holdGrace <= 0 && !UseKeyTracker.isDown(holder))) {
            setHolder(null);
            return;
        }
        entityData.set(FUSE_TOTAL, entityData.get(FUSE_TOTAL) + 1);
        entityData.set(HELD, true);
    }

    private void setHolder(@Nullable PlayerEntity player) {
        holderUUID = player == null ? null : player.getUUID();
        holdGrace = player == null ? 0 : 6;
        entityData.set(HELD, player != null);
    }

    /**
     * Any player (not just the Stand user) can right-click a big pin and keep the button pressed: the fuse is held
     * back for as long as the button stays down (and the player stays within {@link #HOLD_RANGE}). Let go and it ticks again.
     */
    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        if (isSmall() || hand != Hand.MAIN_HAND) {
            return ActionResultType.PASS;
        }
        if (level.isClientSide()) {
            return ActionResultType.SUCCESS;
        }
        if (holderUUID == null || !holderUUID.equals(player.getUUID())) {
            setHolder(player);
            playSound(SoundEvents.IRON_DOOR_CLOSE, 0.6F, 1.7F);
        } else {
            holdGrace = 6; // still pressing - repeated clicks keep the grace window fresh
        }
        return ActionResultType.CONSUME;
    }

    @Override
    public boolean isPickable() {
        return !isSmall() && getState() != STATE_FLYING;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        return true; // left click passes through instead of hurting the pin
    }

    @Override
    public boolean hurt(net.minecraft.util.DamageSource source, float amount) {
        return false;
    }

    // ------------------------------------------------------------------------------------------------ item storage

    /**
     * Everything needed to re-create this pin later (used while a bombed item is carried). The fuse is stored as an
     * absolute world game time ("Detonate") so it keeps running without touching the stack every tick.
     */
    public CompoundNBT writeStored() {
        CompoundNBT tag = new CompoundNBT();
        tag.putLong("Detonate", level.getGameTime() + Math.max(getFuseLeft(), 1));
        tag.putFloat("Damage", damage);
        tag.putFloat("Radius", radius);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        tag.putLong("Uid", getUUID().getLeastSignificantBits()); // keeps two bombed stacks from merging
        return tag;
    }

    /** Applies data written by {@link #writeStored()} (running or frozen). */
    public void readStored(CompoundNBT tag, long now) {
        int fuse = tag.contains("Frozen") ? tag.getInt("Frozen") : (int) (tag.getLong("Detonate") - now);
        configure(tag.getFloat("Damage"), tag.getFloat("Radius"), Math.max(fuse, 1), false);
        ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    public boolean isAttachedTo(Entity e) {
        return getState() == STATE_ATTACHED && entityData.get(TARGET_ID) == e.getId();
    }

    private boolean canHitEntity(Entity e) {
        if (!e.isAlive() || e.isSpectator() || e instanceof ClockBombEntity) {
            return false;
        }
        if (!(e.isPickable() || e instanceof ItemEntity)) {
            return false;
        }
        if (e instanceof StandEntity && isOwnedBy(((StandEntity) e).getUser())) {
            return false;
        }
        return !(tickCount < 6 && isOwnedBy(e)); // don't glue the pin to the thrower
    }

    private void explode() {
        if (level instanceof ServerWorld) {
            BombUtil.detonate((ServerWorld) level, getCenter(), radius, damage, getOwner(), this, isSmall());
        }
        remove();
    }

    // ------------------------------------------------------------------------------------------------ misc

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");
        if (tag.hasUUID("Target")) targetUUID = tag.getUUID("Target");
        entityData.set(FUSE_TOTAL, tag.getInt("FuseTotal"));
        entityData.set(STATE, tag.getByte("State"));
        entityData.set(FACING, Direction.from3DDataValue(tag.getByte("Facing")));
        entityData.set(SMALL, tag.getBoolean("Small"));
        tickCount = tag.getInt("Age");
        damage = tag.getFloat("Damage");
        radius = tag.getFloat("Radius");
        attachOffset = new Vector3d(tag.getDouble("OffX"), tag.getDouble("OffY"), tag.getDouble("OffZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        if (targetUUID != null) tag.putUUID("Target", targetUUID);
        tag.putInt("FuseTotal", entityData.get(FUSE_TOTAL));
        tag.putByte("State", getState());
        tag.putByte("Facing", (byte) getFacing().get3DDataValue());
        tag.putBoolean("Small", isSmall());
        tag.putInt("Age", tickCount);
        tag.putFloat("Damage", damage);
        tag.putFloat("Radius", radius);
        tag.putDouble("OffX", attachOffset.x);
        tag.putDouble("OffY", attachOffset.y);
        tag.putDouble("OffZ", attachOffset.z);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
