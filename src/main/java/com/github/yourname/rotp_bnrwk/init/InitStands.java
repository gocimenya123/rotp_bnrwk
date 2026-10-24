package com.github.yourname.rotp_bnrwk.init;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.action.stand.StandEntityBlock;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.init.power.stand.EntityStandRegistryObject;
import com.github.standobyte.jojo.init.power.stand.ModStandsInit;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.power.impl.stand.type.EntityStandType;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.github.yourname.rotp_bnrwk.BnrwkAddon;
import com.github.yourname.rotp_bnrwk.action.BnrwkBombThrow;
import com.github.yourname.rotp_bnrwk.action.BnrwkEntityBomb;
import com.github.yourname.rotp_bnrwk.action.BnrwkItemBomb;
import com.github.yourname.rotp_bnrwk.action.BnrwkPlantBomb;
import com.github.yourname.rotp_bnrwk.action.BnrwkSmokeBomb;
import com.github.yourname.rotp_bnrwk.entity.stand.BnrwkEntity;

import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Registers the Stand type, its entity type and all of its actions.
 * Hotbar order = order of the arguments in leftClickHotbar / rightClickHotbar.
 */
public class InitStands {
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<Action<?>> ACTIONS = DeferredRegister.create(
            (Class<Action<?>>) ((Class<?>) Action.class), BnrwkAddon.MOD_ID);
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<StandType<?>> STANDS = DeferredRegister.create(
            (Class<StandType<?>>) ((Class<?>) StandType.class), BnrwkAddon.MOD_ID);

    // ======================================== Boku no Rhythm wo Kiitekure ========================================

    // --- Basic melee ---
    public static final RegistryObject<StandEntityAction> BNRWK_PUNCH = ACTIONS.register("bnrwk_punch",
            () -> new StandEntityLightAttack(new StandEntityLightAttack.Builder()));

    public static final RegistryObject<StandEntityAction> BNRWK_BLOCK = ACTIONS.register("bnrwk_block",
            StandEntityBlock::new);

    // --- Contact bombs ---
    /** Touch an entity / block / fluid / item: plant a clock pin that springs up and explodes. */
    public static final RegistryObject<StandEntityAction> BNRWK_PLANT_BOMB = ACTIONS.register("bnrwk_plant_bomb",
            () -> new BnrwkPlantBomb(new StandEntityAction.Builder()
                    .staminaCost(100)
                    .standPerformDuration(6)
                    .cooldown(30)
                    .resolveLevelToUnlock(0)));

    /** Shotgun-like spread of clock pins that stick to whatever they hit. */
    public static final RegistryObject<StandEntityAction> BNRWK_BOMB_THROW = ACTIONS.register("bnrwk_bomb_throw",
            () -> new BnrwkBombThrow(new StandEntityAction.Builder()
                    .staminaCost(200)
                    .standPerformDuration(8)
                    .cooldown(100)
                    .resolveLevelToUnlock(1)));

    /** Smoke cloud filled with many tiny bombs (area denial). */
    public static final RegistryObject<StandEntityAction> BNRWK_SMOKE_BOMB = ACTIONS.register("bnrwk_smoke_bomb",
            () -> new BnrwkSmokeBomb(new StandEntityAction.Builder()
                    .staminaCost(300)
                    .standPerformDuration(10)
                    .cooldown(240)
                    .resolveLevelToUnlock(2)));

    /** Turns one held item into a bomb and tosses it. */
    public static final RegistryObject<StandEntityAction> BNRWK_ITEM_BOMB = ACTIONS.register("bnrwk_item_bomb",
            () -> new BnrwkItemBomb(new StandEntityAction.Builder()
                    .staminaCost(120)
                    .standPerformDuration(6)
                    .cooldown(60)
                    .resolveLevelToUnlock(1)));

    /** Plants a pin on the mob / player / item in the line of sight, up to 24 blocks away. */
    public static final RegistryObject<StandEntityAction> BNRWK_ENTITY_BOMB = ACTIONS.register("bnrwk_entity_bomb",
            () -> new BnrwkEntityBomb(new StandEntityAction.Builder()
                    .staminaCost(150)
                    .standPerformDuration(6)
                    .cooldown(60)
                    .resolveLevelToUnlock(1)));

    // --- Stand type ---
    public static final EntityStandRegistryObject<EntityStandType<StandStats>, StandEntityType<BnrwkEntity>> BNRWK =
            new EntityStandRegistryObject<>("bnrwk",
                    STANDS,
                    () -> new EntityStandType.Builder<StandStats>()
                            .color(0x8C9A7B)
                            .storyPartName(ModStandsInit.PART_7_NAME)
                            .leftClickHotbar(
                                    BNRWK_PUNCH.get())
                            .rightClickHotbar(
                                    BNRWK_PLANT_BOMB.get(),
                                    BNRWK_BOMB_THROW.get(),
                                    BNRWK_SMOKE_BOMB.get(),
                                    BNRWK_ITEM_BOMB.get(),
                                    BNRWK_ENTITY_BOMB.get(),
                                    BNRWK_BLOCK.get())
                            .defaultStats(StandStats.class, new StandStats.Builder()
                                    .power(8.0)
                                    .speed(10.0)
                                    .range(2.0, 4.0)
                                    .durability(9.0)
                                    .precision(8.0)
                                    .randomWeight(1)) // > 0 => can be rolled from a Stand Arrow
                            .build(),

                    InitEntities.ENTITIES,
                    () -> new StandEntityType<BnrwkEntity>(BnrwkEntity::new, 0.65F, 1.95F))
                    .withDefaultStandAttributes();
}
