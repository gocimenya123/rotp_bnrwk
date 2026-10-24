package com.github.yourname.rotp_bnrwk.network;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.github.yourname.rotp_bnrwk.BnrwkAddon;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side record of which players are currently holding the right mouse button (reported by the client). */
@Mod.EventBusSubscriber(modid = BnrwkAddon.MOD_ID)
public class UseKeyTracker {
    private static final Set<UUID> DOWN = ConcurrentHashMap.newKeySet();

    public static void set(UUID player, boolean down) {
        if (down) {
            DOWN.add(player);
        } else {
            DOWN.remove(player);
        }
    }

    public static boolean isDown(PlayerEntity player) {
        return player != null && DOWN.contains(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DOWN.remove(event.getPlayer().getUUID());
    }
}
