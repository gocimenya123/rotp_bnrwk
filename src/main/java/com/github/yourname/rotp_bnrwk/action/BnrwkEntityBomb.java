package com.github.yourname.rotp_bnrwk.action;

import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;
import com.github.yourname.rotp_bnrwk.init.InitSounds;
import com.github.yourname.rotp_bnrwk.util.BombUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

/**
 * Entity Bomb: plants a clock pin on the mob / player / item you are looking at, within {@link #RANGE} blocks.
 * Walls block the aim.
 */
public class BnrwkEntityBomb extends StandEntityAction {
    public static final float DAMAGE = 8.0F;
    public static final float RADIUS = 2.2F;
    public static final int FUSE_TICKS = 100;
    public static final double RANGE = 3.5D;

    public BnrwkEntityBomb(StandEntityAction.Builder builder) {
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
        if (BombUtil.countPlanted(world, user) >= BombUtil.MAX_PLANTED_BOMBS) {
            standEntity.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.2F);
            return;
        }
        BombUtil.Touch touch = BombUtil.touchEntity(world, user, user.getEyePosition(1.0F), user.getViewVector(1.0F), RANGE);
        if (touch == null || touch.entity == null) {
            standEntity.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.2F);
            if (user instanceof PlayerEntity) {
                ((PlayerEntity) user).displayClientMessage(
                        new TranslationTextComponent("action.rotp_bnrwk.bnrwk_entity_bomb.no_target"), true);
            }
            return;
        }
        ClockBombEntity bomb = new ClockBombEntity(world);
        bomb.setOwner(user);
        bomb.configure(DAMAGE, RADIUS, FUSE_TICKS, false);
        bomb.attachTo(touch.entity, touch.pos, touch.face);
        world.addFreshEntity(bomb);
        standEntity.playSound(InitSounds.PLANT.get(), 1.0F, 1.0F);
    }
}
