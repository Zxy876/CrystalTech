package com.crystaltech.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.crystaltech.CrystalTech;
import com.crystaltech.capability.ICrystalStage;
import com.crystaltech.protocol.ManifestationIntent;
import com.crystaltech.protocol.ManifestationIntentLogger;
import com.crystaltech.protocol.ManifestationIntentService;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Developer command for queuing manifestation intents without the plugin.
 */
@Mod.EventBusSubscriber(modid = CrystalTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManifestationIntentDebugCommands {
    private ManifestationIntentDebugCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("crystalintent")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("grant")
                        .then(Commands.argument("stage", IntegerArgumentType.integer(ICrystalStage.MIN_STAGE, ICrystalStage.MAX_STAGE))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int stage = IntegerArgumentType.getInteger(ctx, "stage");
                                    ManifestationIntent intent = new ManifestationIntent(
                                            "debug-" + UUID.randomUUID(),
                                            "0.1.0",
                                            "debug",
                                            "debug",
                                            stage,
                                            "debug",
                                            List.of(),
                                            List.of("debug command"),
                                            Instant.now(),
                                            Instant.now().plusSeconds(300),
                                            "debug-signature"
                                    );
                                    ManifestationIntentService.getInstance().submitDebugIntent(player.getUUID(), intent);
                                    ManifestationIntentLogger.logReceived(intent, player.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal("Queued manifest intent for stage " + stage), true);
                                    return 1;
                                }))));
    }
}
