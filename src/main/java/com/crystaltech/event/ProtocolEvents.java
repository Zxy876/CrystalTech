package com.crystaltech.event;

import java.time.Instant;

import com.crystaltech.CrystalTech;
import com.crystaltech.protocol.ManifestationIntentService;
import com.crystaltech.protocol.ProtocolTelemetryService;

import net.minecraft.server.MinecraftServer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Periodically drives the manifestation intent service on the server thread.
 */
@Mod.EventBusSubscriber(modid = CrystalTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProtocolEvents {
    private ProtocolEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Instant now = Instant.now();
        ManifestationIntentService.getInstance().consume(now);
        MinecraftServer server = event.getServer();
        if (server != null) {
            ProtocolTelemetryService.getInstance().tick(server, server.getTickCount(), now);
        }
    }
}
