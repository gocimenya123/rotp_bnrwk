package com.github.yourname.rotp_bnrwk.util;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mc.damage.StandEntityDamageSource;
import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;
import com.github.yourname.rotp_bnrwk.init.InitSounds;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/**
 * Shared helpers: "touch" ray tracing (entities, items, blocks AND fluids) and the bomb explosion itself.
 */
public final class BombUtil {
    /** Max amount of non-smoke bombs a single user can have planted at once. */
    public static final int MAX_PLANTED_BOMBS = 8;

    /** NBT key on an ItemStack: list of clock pins stored in it while a player carries the item. */
    public static final String STORED_TAG = "BnrwkPins";

    /** Result of a "touch" - what the Stand's hand reached. */
    public static class Touch {
        public final Vector3d pos;       // exact point that was touched
        public final Direction face;     // outward normal of the touched surface
        @Nullable
        public final Entity entity;      // null if a block / fluid / nothing (end of range) was touched

        public Touch(Vector3d pos, Direction face, @Nullable Entity entity) {
            this.pos = pos;
            this.face = face;
            this.entity = entity;
        }
    }

    /**
     * Ray traces from {@code start} along {@code look}. Hits living entities, item entities, any pickable entity,
     * solid blocks and fluids (water/lava) - whatever is closest. Returns null if nothing is in range.
     */
    @Nullable
    public static Touch touch(World world, LivingEntity user, Vector3d start, Vector3d look, double range) {
        Vector3d end = start.add(look.scale(range));
        BlockRayTraceResult blockHit = world.clip(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.ANY, user));
        double bestSqr = range * range;
        boolean blockCloser = blockHit.getType() != RayTraceResult.Type.MISS;
        if (blockCloser) {
            bestSqr = start.distanceToSqr(blockHit.getLocation());
        }

        Entity bestEntity = null;
        Vector3d bestEntityPos = null;
        AxisAlignedBB search = user.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5D);
        List<Entity> candidates = world.getEntities(user, search, e -> isTouchable(e, user));
        for (Entity e : candidates) {
            AxisAlignedBB box = e.getBoundingBox().inflate(0.2D);
            Optional<Vector3d> clip = box.clip(start, end);
            if (box.contains(start)) {
                clip = Optional.of(start);
            }
            if (clip.isPresent()) {
                double d = start.distanceToSqr(clip.get());
                if (d <= bestSqr) {
                    bestSqr = d;
                    bestEntity = e;
                    bestEntityPos = clip.get();
                    blockCloser = false;
                }
            }
        }

        if (bestEntity != null) {
            return new Touch(bestEntityPos, faceFromEntityHit(bestEntity, bestEntityPos), bestEntity);
        }
        if (blockCloser) {
            Direction face = blockHit.getDirection();
            Vector3d p = blockHit.getLocation().add(face.getStepX() * 0.04D, face.getStepY() * 0.04D, face.getStepZ() * 0.04D);
            return new Touch(p, face, null);
        }
        return null;
    }

    /**
     * Like {@link #touch} but only for entities (living or items) up to {@code range} blocks away, with a generous
     * aim tolerance, and blocked by solid walls. Returns null if there is no entity in the line of sight.
     */
    @Nullable
    public static Touch touchEntity(World world, LivingEntity user, Vector3d start, Vector3d look, double range) {
        Vector3d end = start.add(look.scale(range));
        BlockRayTraceResult blockHit = world.clip(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        double limitSqr = range * range;
        if (blockHit.getType() != RayTraceResult.Type.MISS) {
            limitSqr = start.distanceToSqr(blockHit.getLocation());
        }
        Entity best = null;
        Vector3d bestPos = null;
        double bestSqr = limitSqr;
        AxisAlignedBB search = user.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D);
        for (Entity e : world.getEntities(user, search, en -> isTouchable(en, user) && !(en instanceof ClockBombEntity))) {
            AxisAlignedBB box = e.getBoundingBox().inflate(0.35D);
            Optional<Vector3d> clip = box.contains(start) ? Optional.of(start) : box.clip(start, end);
            if (clip.isPresent()) {
                double d = start.distanceToSqr(clip.get());
                if (d <= bestSqr) {
                    bestSqr = d;
                    best = e;
                    bestPos = clip.get();
                }
            }
        }
        if (best == null) {
            return null;
        }
        // pin goes onto the target's body, not onto the inflated aim box
        Vector3d onBody = best.getBoundingBox().clip(start, end).orElse(best.position().add(0, best.getBbHeight() * 0.5D, 0));
        return new Touch(onBody, faceFromEntityHit(best, onBody), best);
    }

    private static boolean isTouchable(Entity e, LivingEntity user) {
        if (!e.isAlive() || e.isSpectator() || e == user || e instanceof ClockBombEntity) {
            return false;
        }
        if (e instanceof StandEntity && ((StandEntity) e).getUser() == user) {
            return false;
        }
        return e.isPickable() || e instanceof ItemEntity;
    }

    /** Which side of an entity's bounding box a hit point is closest to. */
    public static Direction faceFromEntityHit(Entity target, Vector3d hit) {
        AxisAlignedBB box = target.getBoundingBox();
        Vector3d d = hit.subtract(box.getCenter());
        double nx = d.x / Math.max(box.getXsize() * 0.5D, 1.0E-4D);
        double ny = d.y / Math.max(box.getYsize() * 0.5D, 1.0E-4D);
        double nz = d.z / Math.max(box.getZsize() * 0.5D, 1.0E-4D);
        double ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        if (ax >= ay && ax >= az) {
            return nx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (ay >= az) {
            return ny > 0 ? Direction.UP : Direction.DOWN;
        }
        return nz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** How many large (player-planted) bombs this user currently has out. */
    public static int countPlanted(World world, LivingEntity user) {
        return world.getEntitiesOfClass(ClockBombEntity.class, user.getBoundingBox().inflate(128.0D),
                b -> b.isOwnedBy(user) && !b.isSmall()).size();
    }

    // ------------------------------------------------------------------------------------------------------------
    //  Explosion
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Detonates a bomb: particles, sound, damage + knockback (through RotP's Stand damage source when the owner
     * has a manifested Stand, so resolve / Stand-related damage rules apply) and a small crater of weak blocks.
     * The owner and the owner's Stand are never hurt.
     */
    public static void detonate(ServerWorld world, Vector3d pos, float radius, float damage,
                                @Nullable LivingEntity owner, @Nullable Entity bomb, boolean small) {
        // --- visuals & sound ---
        world.sendParticles(small ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        world.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, small ? 6 : 20,
                radius * 0.3D, radius * 0.3D, radius * 0.3D, 0.02D);
        world.playSound(null, pos.x, pos.y, pos.z, InitSounds.BOMB_EXPLODE.get(), SoundCategory.BLOCKS,
                small ? 1.0F : 2.5F, small ? 1.15F : 0.95F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS,
                small ? 1.5F : 3.5F, (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * (small ? 0.9F : 0.7F));

        // --- entity damage ---
        DamageSource source = createDamageSource(owner);
        AxisAlignedBB area = new AxisAlignedBB(pos, pos).inflate(radius);
        for (Entity e : world.getEntities(bomb, area, entity -> entity.isAlive() && !entity.isSpectator())) {
            if (e instanceof ClockBombEntity || e instanceof net.minecraft.entity.projectile.ProjectileEntity) {
                continue;
            }
            if (owner != null && (e == owner || (e instanceof StandEntity && ((StandEntity) e).getUser() == owner))) {
                continue;
            }
            Vector3d center = e.position().add(0, e.getBbHeight() * 0.5D, 0);
            double dist = center.distanceTo(pos) / radius;
            if (dist > 1.0D) {
                continue;
            }
            float seen = Explosion.getSeenPercent(pos, e);
            if (seen <= 0.0F) {
                continue;
            }
            float amount = damage * (float) (1.0D - dist * 0.5D) * (0.4F + 0.6F * seen);
            if (e instanceof LivingEntity) {
                e.invulnerableTime = 0; // every pin must be able to hurt, even right after another one
            }
            boolean hurt;
            if (e instanceof PlayerEntity) {
                hurt = hurtPlayer(world, (PlayerEntity) e, owner, amount);
            } else {
                hurt = e.hurt(source, amount);
            }
            if (hurt && e instanceof LivingEntity) {
                Vector3d dir = center.subtract(pos);
                dir = dir.lengthSqr() < 1.0E-4D ? new Vector3d(0, 1, 0) : dir.normalize();
                double kb = (small ? 0.35D : 1.1D) * (1.0D - dist);
                e.setDeltaMovement(e.getDeltaMovement().add(dir.x * kb, 0.25D + dir.y * kb * 0.6D, dir.z * kb));
                e.hurtMarked = true;
            }
        }

        // --- tiny crater: only soft blocks, no tile entities, respects mobGriefing ---
        if (!small && world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            int r = Math.max(1, (int) Math.floor(radius * 0.6F));
            BlockPos origin = new BlockPos(pos);
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
                if (p.distSqr(origin) > r * r) continue;
                BlockState state = world.getBlockState(p);
                if (state.isAir(world, p) || world.getBlockEntity(p) != null) continue;
                float hardness = state.getDestroySpeed(world, p);
                if (hardness >= 0.0F && hardness <= 0.8F) {
                    world.destroyBlock(p.immutable(), true);
                }
            }
        }
    }

    /** Explosion damage that is allowed to bypass armor - used when the normal hit gets swallowed. */
    private static class BombDamage extends EntityDamageSource {
        BombDamage(@Nullable Entity owner, boolean bypassArmor) {
            super("bnrwk_bomb", owner);
            setExplosion();
            if (bypassArmor) {
                bypassArmor();
            }
        }
    }

    /**
     * Players: plain explosion damage credited to the owner. If that is somehow swallowed (Stand damage rules, ...) and
     * the cause isn't PvP being disabled or creative / invulnerability, the blast goes through armor instead.
     */
    private static boolean hurtPlayer(ServerWorld world, PlayerEntity player, @Nullable LivingEntity owner, float amount) {
        if (player.hurt(new BombDamage(owner, false), amount)) {
            return true;
        }
        if (player.abilities.invulnerable || player.isSpectator() || player.isInvulnerable()
                || (owner instanceof PlayerEntity && world.getServer() != null && !world.getServer().isPvpAllowed())) {
            return false;
        }
        player.invulnerableTime = 0;
        return player.hurt(new BombDamage(owner, true), amount);
    }

    private static DamageSource createDamageSource(@Nullable LivingEntity owner) {
        if (owner != null) {
            IStandPower power = IStandPower.getStandPowerOptional(owner).resolve().orElse(null);
            if (power != null && power.getStandManifestation() instanceof StandEntity) {
                return new StandEntityDamageSource("bnrwk_bomb", (StandEntity) power.getStandManifestation(), power);
            }
        }
        return DamageSource.explosion(owner);
    }
}
