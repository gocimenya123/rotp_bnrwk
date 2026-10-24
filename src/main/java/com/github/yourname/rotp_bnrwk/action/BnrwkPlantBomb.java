package com.github.yourname.rotp_bnrwk.action;

import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;
import com.github.yourname.rotp_bnrwk.init.InitSounds;
import com.github.yourname.rotp_bnrwk.util.BombUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * Plant Bomb: whatever the Stand touches (mob, player, dropped item, block, water surface, thin air in smoke...)
 * gets a clock pin. ~4 seconds later the pin springs up and detonates. Up to
 * {@link BombUtil#MAX_PLANTED_BOMBS} pins can be out at once.
 */
public class BnrwkPlantBomb extends StandEntityAction {
    public static final float DAMAGE = 18.0F;   // ~9 hearts at the centre
    public static final float RADIUS = 3.2F;
    public static final int FUSE_TICKS = 100;   // 5 seconds
    private static final double REACH = 4.0D;

    public BnrwkPlantBomb(StandEntityAction.Builder builder) {
        super(builder);
    }

    /** Called once when the Stand's hand "touches" (the perform phase starts). */
    @Override
    public void standPerform(World world, StandEntity standEntity, IStandPower userPower, StandEntityTask task) {
        if (world.isClientSide()) {
            return;
        }
        LivingEntity user = userPower.getUser();
        if (user == null) {
            return;
        }
        if (BombUtil.countPlanted(world, user) >= BombUtil.MAX_PLANTED_BOMBS) {
            standEntity.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.2F);
            return;
        }

        Vector3d start = standEntity.getEyePosition(1.0F);
        BombUtil.Touch touch = BombUtil.touch(world, user, start, user.getViewVector(1.0F), REACH);
        if (touch == null) {
            return;
        }

        ClockBombEntity bomb = new ClockBombEntity(world);
        bomb.setOwner(user);
        bomb.configure(DAMAGE, RADIUS, FUSE_TICKS, false);
        if (touch.entity != null) {
            bomb.attachTo(touch.entity, touch.pos, touch.face);
        } else {
            bomb.stickAt(touch.pos, touch.face);
        }
        world.addFreshEntity(bomb);
        standEntity.playSound(InitSounds.PLANT.get(), 1.0F, 1.0F);
    }
}
