package com.github.yourname.rotp_bnrwk.network;

import com.github.yourname.rotp_bnrwk.BnrwkAddon;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class BnrwkNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BnrwkAddon.MOD_ID, "main"), () -> PROTOCOL, s -> true, s -> true);

    private static boolean registered = false;

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(0, UseKeyPacket.class, UseKeyPacket::encode, UseKeyPacket::decode, UseKeyPacket::handle);
    }
}
