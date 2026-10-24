package com.github.yourname.rotp_bnrwk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;
import com.github.yourname.rotp_bnrwk.network.UseKeyTracker;
import com.github.yourname.rotp_bnrwk.util.BombUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bombed items.
 *
 *  - Anyone can pick a bombed item up: the pin moves into the stack's NBT ("BnrwkPins") and KEEPS TICKING in the
 *    inventory. The only way to delay it is to hold the item in your hand (selected slot / off hand) with the right
 *    mouse button pressed - the fuse is frozen until you let go. When it runs out the item blows up in the inventory.
 *  - Dropping the item (Q, death, ...) re-attaches the pin to the dropped item with its remaining fuse.
 *  - Placing a bombed block leaves the pin stuck on the placed block.
 *
 * NBT per pin: Detonate (absolute game time) while running, or Frozen (ticks left) while held; Damage, Radius, Owner, Uid.
 * Storing an absolute time means the stack only changes when the freeze state flips, not every tick.
 */
@Mod.EventBusSubscriber(modid = BnrwkAddon.MOD_ID)
public class BnrwkEvents {
    /** An item that sat in a container (no ticking there) gets this much fuse when it shows up overdue. */
    private static final int OVERDUE_GRACE_TICKS = 40;

    private static final Map<UUID, PendingPlace> PENDING_PLACE = new HashMap<>();

    private static class PendingPlace {
        final ListNBT pins;
        final long time;
        final Direction face;
        final Hand hand;

        PendingPlace(ListNBT pins, long time, Direction face, Hand hand) {
            this.pins = pins;
            this.time = time;
            this.face = face;
            this.hand = hand;
        }
    }

    // ------------------------------------------------------------------------------------------ pick up

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        ItemEntity itemEntity = event.getItem();
        World world = itemEntity.level;
        if (world.isClientSide()) {
            return;
        }
        List<ClockBombEntity> pins = world.getEntitiesOfClass(ClockBombEntity.class,
                itemEntity.getBoundingBox().inflate(1.5D), p -> !p.isSmall() && p.isAttachedTo(itemEntity));
        if (pins.isEmpty()) {
            return;
        }
        ItemStack stack = itemEntity.getItem();
        if (stack.getCount() != 1) {
            event.setCanceled(true); // a pin belongs to exactly one item; the pile stays on the ground with it
            return;
        }
        if (event.getPlayer().inventory.getFreeSlot() < 0) {
            return; // would not fit anyway - leave everything where it is
        }
        CompoundNBT tag = stack.getOrCreateTag();
        ListNBT stored = tag.getList(BombUtil.STORED_TAG, 10);
        for (ClockBombEntity pin : pins) {
            stored.add(pin.writeStored());
            pin.remove();
        }
        tag.put(BombUtil.STORED_TAG, stored);
    }

    // ------------------------------------------------------------------------------------------ carrying

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide()) {
            return;
        }
        PlayerEntity player = event.player;
        PlayerInventory inv = player.inventory;
        boolean rmb = UseKeyTracker.isDown(player);
        long now = player.level.getGameTime();
        for (int i = 0; i < inv.items.size(); i++) {
            tickStack(player, inv.items.get(i), rmb && i == inv.selected, now);
        }
        for (int i = 0; i < inv.offhand.size(); i++) {
            tickStack(player, inv.offhand.get(i), rmb, now);
        }
        for (int i = 0; i < inv.armor.size(); i++) {
            tickStack(player, inv.armor.get(i), false, now);
        }
    }

    private static void tickStack(PlayerEntity player, ItemStack stack, boolean holdIt, long now) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(BombUtil.STORED_TAG, 9)) {
            return;
        }
        ListNBT list = stack.getTag().getList(BombUtil.STORED_TAG, 10);
        boolean explode = false;
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT pin = list.getCompound(i);
            boolean frozen = pin.contains("Frozen");
            if (holdIt && !frozen) {
                pin.putInt("Frozen", (int) Math.max(1L, pin.getLong("Detonate") - now));
                pin.remove("Detonate");
            } else if (!holdIt && frozen) {
                pin.putLong("Detonate", now + pin.getInt("Frozen"));
                pin.remove("Frozen");
            }
            if (!pin.contains("Frozen")) {
                long detonate = pin.getLong("Detonate");
                if (now - detonate > 5L) {
                    detonate = now + OVERDUE_GRACE_TICKS; // it was sitting in a chest, don't blow up on retrieval
                    pin.putLong("Detonate", detonate);
                }
                if (now >= detonate) {
                    explode = true;
                }
            }
        }
        if (explode && player.level instanceof ServerWorld) {
            ServerWorld world = (ServerWorld) player.level;
            Vector3d at = player.position().add(0.0D, player.getBbHeight() * 0.6D, 0.0D);
            for (int i = 0; i < list.size(); i++) {
                CompoundNBT pin = list.getCompound(i);
                LivingEntity owner = null;
                if (pin.hasUUID("Owner")) {
                    Entity e = world.getEntity(pin.getUUID("Owner"));
                    owner = e instanceof LivingEntity ? (LivingEntity) e : null;
                }
                BombUtil.detonate(world, at, pin.getFloat("Radius"), pin.getFloat("Damage"), owner, null, false);
            }
            stack.shrink(stack.getCount()); // the item is destroyed together with its pin
        }
    }

    // ------------------------------------------------------------------------------------------ dropping

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        rearm(event.getEntityItem());
    }

    @SubscribeEvent
    public static void onDeathDrops(LivingDropsEvent event) {
        for (ItemEntity drop : event.getDrops()) {
            rearm(drop);
        }
    }

    /** Moves the stored pins of a freshly dropped stack back onto the item entity. */
    private static void rearm(ItemEntity itemEntity) {
        World world = itemEntity.level;
        if (world.isClientSide()) {
            return;
        }
        ItemStack stack = itemEntity.getItem();
        if (!stack.hasTag() || !stack.getTag().contains(BombUtil.STORED_TAG, 9)) {
            return;
        }
        ListNBT stored = stack.getTag().getList(BombUtil.STORED_TAG, 10);
        stack.getTag().remove(BombUtil.STORED_TAG);
        if (stack.getTag().isEmpty()) {
            stack.setTag(null);
        }
        long now = world.getGameTime();
        for (int i = 0; i < stored.size(); i++) {
            ClockBombEntity pin = new ClockBombEntity(world);
            pin.readStored(stored.getCompound(i), now);
            pin.attachTo(itemEntity, itemEntity.position().add(0.0D, itemEntity.getBbHeight() * 0.5D, 0.0D), Direction.UP, false);
            world.addFreshEntity(pin);
        }
    }

    // ------------------------------------------------------------------------------------------ placing

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof BlockItem && stack.hasTag() && stack.getTag().contains(BombUtil.STORED_TAG, 9)) {
            PENDING_PLACE.put(event.getPlayer().getUUID(), new PendingPlace(
                    stack.getTag().getList(BombUtil.STORED_TAG, 10).copy(), event.getWorld().getGameTime(),
                    event.getFace(), event.getHand()));
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        IWorld iworld = event.getWorld();
        if (iworld.isClientSide() || !(iworld instanceof World) || !(event.getEntity() instanceof PlayerEntity)) {
            return;
        }
        World world = (World) iworld;
        PlayerEntity player = (PlayerEntity) event.getEntity();
        PendingPlace pending = PENDING_PLACE.remove(player.getUUID());
        if (pending == null || world.getGameTime() - pending.time > 1L) {
            return;
        }
        // creative mode doesn't use the stack up - make sure the pins aren't duplicated
        ItemStack inHand = player.getItemInHand(pending.hand);
        if (!inHand.isEmpty() && inHand.hasTag()) {
            inHand.getTag().remove(BombUtil.STORED_TAG);
            if (inHand.getTag().isEmpty()) {
                inHand.setTag(null);
            }
        }
        Direction face = pending.face != null ? pending.face : Direction.UP;
        Vector3d at = Vector3d.atCenterOf(event.getPos()).add(face.getStepX() * 0.54D, face.getStepY() * 0.54D, face.getStepZ() * 0.54D);
        long now = world.getGameTime();
        for (int i = 0; i < pending.pins.size(); i++) {
            ClockBombEntity pin = new ClockBombEntity(world);
            pin.readStored(pending.pins.getCompound(i), now);
            pin.stickAt(at, face);
            world.addFreshEntity(pin);
        }
    }

    // ------------------------------------------------------------------------------------------ tooltip

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.hasTag() && stack.getTag().contains(BombUtil.STORED_TAG, 9)) {
            event.getToolTip().add(new TranslationTextComponent("tooltip.rotp_bnrwk.armed_item").withStyle(TextFormatting.RED));
            event.getToolTip().add(new TranslationTextComponent("tooltip.rotp_bnrwk.armed_item.hint").withStyle(TextFormatting.GRAY));
        }
    }
}
