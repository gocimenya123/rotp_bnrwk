package com.github.yourname.rotp_bnrwk.action;

import java.util.Random;

import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;
import com.github.yourname.rotp_bnrwk.init.InitSounds;
import com.github.yourname.rotp_bnrwk.util.BombUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * Smoke Bomb: a cloud of smoke is thrown at the aimed spot. Dozens of tiny clock pins are planted inside the smoke
 * itself and go off one after another a few seconds later - area denial rather than burst damage.
 */
public class BnrwkSmokeBomb extends StandEntityAction {
    public static final int PINS = 14;
    public static final float PIN_DAMAGE = 4.5F;
    public static final float PIN_RADIUS = 1.9F;
    public static final float CLOUD_RADIUS = 3.8F;
    private static final double RANGE = 12.0D;

    public BnrwkSmokeBomb(StandEntityAction.Builder builder) {
        super(builder);
    }

    @Override
    public void standPerform(World world, StandEntity standEntity, IStandPower userPower, StandEntityTask task) {
        if (world.isClientSide()) {
            return;
        }
        LivingEntity user = userPower.getUser();
        if (user == null) {
            return;
        }
        Vector3d start = standEntity.getEyePosition(1.0F);
        Vector3d look = user.getViewVector(1.0F);
        BombUtil.Touch touch = BombUtil.touch(world, user, start, look, RANGE);
        Vector3d center = touch != null
                ? touch.pos.add(touch.face.getStepX() * 0.5D, touch.face.getStepY() * 0.5D + 0.8D, touch.face.getStepZ() * 0.5D)
                : start.add(look.scale(RANGE));

        // The visible smoke (vanilla cloud entity with no potion effects = pure particles)
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, center.x, center.y, center.z);
        cloud.setOwner(user);
        cloud.setParticle(ParticleTypes.LARGE_SMOKE);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setDuration(110);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-0.012F);
        world.addFreshEntity(cloud);

        // The pins hidden inside it
        Random rand = world.getRandom();
        for (int i = 0; i < PINS; i++) {
            Vector3d offset = new Vector3d(rand.nextGaussian(), rand.nextGaussian() * 0.5D, rand.nextGaussian())
                    .scale(CLOUD_RADIUS * 0.45D);
            ClockBombEntity pin = new ClockBombEntity(world);
            pin.setOwner(user);
            pin.configure(PIN_DAMAGE, PIN_RADIUS, 35 + rand.nextInt(60), true);
            pin.stickAt(center.add(offset), Direction.getRandom(rand));
            world.addFreshEntity(pin);
        }
        standEntity.playSound(InitSounds.SMOKE.get(), 1.0F, 1.0F);
    }
}
