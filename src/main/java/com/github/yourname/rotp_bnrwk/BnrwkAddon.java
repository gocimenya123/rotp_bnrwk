package com.github.yourname.rotp_bnrwk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.yourname.rotp_bnrwk.init.InitEntities;
import com.github.yourname.rotp_bnrwk.init.InitSounds;
import com.github.yourname.rotp_bnrwk.init.InitStands;
import com.github.yourname.rotp_bnrwk.network.BnrwkNetwork;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Addon entry point: "Boku no Rhythm wo Kiitekure" for Ripples of the Past.
 */
@Mod(BnrwkAddon.MOD_ID)
public class BnrwkAddon {
    public static final String MOD_ID = "rotp_bnrwk";
    private static final Logger LOGGER = LogManager.getLogger();

    public BnrwkAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BnrwkNetwork.init();
        InitEntities.ENTITIES.register(modEventBus);
        InitSounds.SOUNDS.register(modEventBus);
        InitStands.ACTIONS.register(modEventBus);
        InitStands.STANDS.register(modEventBus);
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
