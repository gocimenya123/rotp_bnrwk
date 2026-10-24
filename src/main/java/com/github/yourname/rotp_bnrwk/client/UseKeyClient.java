package com.github.yourname.rotp_bnrwk.client;

import com.github.yourname.rotp_bnrwk.BnrwkAddon;
import com.github.yourname.rotp_bnrwk.network.BnrwkNetwork;
import com.github.yourname.rotp_bnrwk.network.UseKeyPacket;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tells the server when the right mouse button goes down / up (needed to hold pins and bombed items). */
@Mod.EventBusSubscriber(modid = BnrwkAddon.MOD_ID, value = Dist.CLIENT)
public class UseKeyClient {
    private static boolean lastSent = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean down = mc.player != null && mc.screen == null && mc.options.keyUse.isDown();
        if (down != lastSent && mc.getConnection() != null) {
            lastSent = down;
            BnrwkNetwork.CHANNEL.sendToServer(new UseKeyPacket(down));
        } else if (mc.getConnection() == null) {
            lastSent = false;
        }
    }
}
