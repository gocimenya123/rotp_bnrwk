package com.github.yourname.rotp_bnrwk.action;

import java.util.Random;

import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;
import com.github.yourname.rotp_bnrwk.init.InitSounds;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * Bomb Throw: flings several clock pins in a shotgun-like spread. Each pin sticks to the first thing it hits
 * and explodes after its own short fuse, so a point-blank hit stacks several smaller blasts.
 */
public class BnrwkBombThrow extends StandEntityAction {
    public static final int PELLETS = 6;
    public static final float DAMAGE = 8.0F;
    public static final float RADIUS = 2.4F;
    private static final double SPEED = 1.5D;
    private static final double SPREAD = 0.10D;

    public BnrwkBombThrow(StandEntityAction.Builder builder) {
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
        Random rand = world.getRandom();
        Vector3d origin = standEntity.getEyePosition(1.0F);
        Vector3d look = user.getViewVector(1.0F);

        for (int i = 0; i < PELLETS; i++) {
            Vector3d dir = look.add(rand.nextGaussian() * SPREAD, rand.nextGaussian() * SPREAD, rand.nextGaussian() * SPREAD).normalize();
            ClockBombEntity bomb = new ClockBombEntity(world);
            bomb.setOwner(user);
            bomb.configure(DAMAGE, RADIUS, 100, false); // 5 s after being thrown
            bomb.setPos(origin.x, origin.y - 0.15D, origin.z);
            bomb.launch(dir.scale(SPEED * (0.9D + rand.nextDouble() * 0.2D)));
            world.addFreshEntity(bomb);
        }
        standEntity.playSound(InitSounds.THROW.get(), 1.0F, 1.0F);
    }
}
