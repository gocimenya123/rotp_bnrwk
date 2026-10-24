package com.github.yourname.rotp_bnrwk.init;

import com.github.yourname.rotp_bnrwk.BnrwkAddon;
import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class InitEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.ENTITIES, BnrwkAddon.MOD_ID);

    /** The clock-shaped contact pin. Used by all three bomb abilities. */
    public static final RegistryObject<EntityType<ClockBombEntity>> CLOCK_BOMB = ENTITIES.register("clock_bomb",
            () -> EntityType.Builder.<ClockBombEntity>of(ClockBombEntity::new, EntityClassification.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(10)
                    .updateInterval(1) // attached bombs follow their target every tick
                    .setShouldReceiveVelocityUpdates(true)
                    .fireImmune()
                    .noSummon()
                    .build(BnrwkAddon.MOD_ID + ":clock_bomb"));
}
