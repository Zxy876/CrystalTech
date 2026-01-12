package com.crystaltech.network;

import com.crystaltech.CrystalTech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Centralises registration of CrystalTech network packets.
 */
public final class CrystalTechNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CrystalTech.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static boolean registered;

    private CrystalTechNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        int index = 0;
        CHANNEL.messageBuilder(CityPhoneDataMessage.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CityPhoneDataMessage::encode)
                .decoder(CityPhoneDataMessage::decode)
                .consumerMainThread(CityPhoneDataMessage::handle)
                .add();
        registered = true;
        CrystalTech.LOGGER.info("CrystalTech network channel initialised.");
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
