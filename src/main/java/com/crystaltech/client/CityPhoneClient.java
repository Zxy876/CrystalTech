package com.crystaltech.client;

import com.crystaltech.client.gui.CityPhoneScreen;
import com.crystaltech.network.CityPhoneDataMessage.CityPhoneSnapshot;
import net.minecraft.client.Minecraft;

/**
 * Client-side helpers for the CityPhone view.
 */
public final class CityPhoneClient {
    private CityPhoneClient() {
    }

    public static void openCityPhone(CityPhoneSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new CityPhoneScreen(snapshot));
    }
}
