package com.github.yourname.rotp_bnrwk.network;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

/** Client -> server: "the right mouse button is now down / up". */
public class UseKeyPacket {
    private final boolean down;

    public UseKeyPacket(boolean down) {
        this.down = down;
    }

    public static void encode(UseKeyPacket msg, PacketBuffer buf) {
        buf.writeBoolean(msg.down);
    }

    public static UseKeyPacket decode(PacketBuffer buf) {
        return new UseKeyPacket(buf.readBoolean());
    }

    public static void handle(UseKeyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity sender = context.getSender();
            if (sender != null) {
                UseKeyTracker.set(sender.getUUID(), msg.down);
            }
        });
        context.setPacketHandled(true);
    }
}
