package com.crystaltech.event;

import com.crystaltech.CrystalTech;
import com.crystaltech.network.CityPhoneDataMessage;
import com.crystaltech.network.CrystalTechNetwork;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.Instant;

/**
 * Exposes the /cityphone command for viewing manifestation intents.
 */
@Mod.EventBusSubscriber(modid = CrystalTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CityPhoneCommands {
    private CityPhoneCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("cityphone")
                .requires(source -> source.hasPermission(0))
                .executes(CityPhoneCommands::showCityPhone));
    }

    private static int showCityPhone(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CityPhoneDataMessage message = CityPhoneDataMessage.fromPlayer(player, Instant.now());
        CrystalTechNetwork.sendToPlayer(player, message);
        context.getSource().sendSuccess(() -> Component.literal("Opened CityPhone view."), false);
        return 1;
    }
}
