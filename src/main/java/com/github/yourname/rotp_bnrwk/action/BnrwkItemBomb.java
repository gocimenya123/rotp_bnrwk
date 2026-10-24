package com.github.yourname.rotp_bnrwk.action;

import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;
import com.github.yourname.rotp_bnrwk.init.InitSounds;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * Item Bomb: turns ONE item from the user's hand (main hand first, then off hand) into a bomb and tosses it.
 * A clock pin is planted into the thrown item itself, so the item goes off together with the pin
 * (and is destroyed by the blast).
 */
public class BnrwkItemBomb extends StandEntityAction {
    public static final float DAMAGE = 14.0F;
    public static final float RADIUS = 3.0F;
    public static final int FUSE_TICKS = 100;

    public BnrwkItemBomb(StandEntityAction.Builder builder) {
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

        Hand hand = !user.getMainHandItem().isEmpty() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        ItemStack held = user.getItemInHand(hand);
        if (held.isEmpty()) {
            standEntity.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.2F);
            return;
        }

        ItemStack single = held.copy();
        single.setCount(1);
        boolean creative = user instanceof PlayerEntity && ((PlayerEntity) user).abilities.instabuild;
        if (!creative) {
            held.shrink(1);
        }

        Vector3d start = standEntity.getEyePosition(1.0F);
        Vector3d look = user.getViewVector(1.0F);
        ItemEntity item = new ItemEntity(world, start.x, start.y - 0.2D, start.z, single);
        item.setDeltaMovement(look.scale(0.9D).add(0.0D, 0.12D, 0.0D));
        item.setPickUpDelay(15);
        world.addFreshEntity(item);

        ClockBombEntity bomb = new ClockBombEntity(world);
        bomb.setOwner(user);
        bomb.configure(DAMAGE, RADIUS, FUSE_TICKS, false);
        bomb.attachTo(item, item.position().add(0.0D, item.getBbHeight() * 0.5D, 0.0D), Direction.UP);
        world.addFreshEntity(bomb);

        standEntity.playSound(InitSounds.PLANT.get(), 1.0F, 1.1F);
    }
}
