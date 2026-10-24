package com.github.yourname.rotp_bnrwk.client;

import com.github.yourname.rotp_bnrwk.BnrwkAddon;
import com.github.yourname.rotp_bnrwk.client.render.BnrwkRenderer;
import com.github.yourname.rotp_bnrwk.client.render.ClockBombRenderer;
import com.github.yourname.rotp_bnrwk.init.InitEntities;
import com.github.yourname.rotp_bnrwk.init.InitStands;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only registration: entity renderers. */
@Mod.EventBusSubscriber(modid = BnrwkAddon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onFMLClientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(InitStands.BNRWK.getEntityType(), BnrwkRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(InitEntities.CLOCK_BOMB.get(), ClockBombRenderer::new);
    }
}
