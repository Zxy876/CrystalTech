package com.crystaltech.core;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired after a player's crystal stage transitions to a new value.
 */
public class CrystalStageChangedEvent extends Event {
    private final Player player;
    private final int oldStage;
    private final int newStage;

    public CrystalStageChangedEvent(Player player, int oldStage, int newStage) {
        this.player = player;
        this.oldStage = oldStage;
        this.newStage = newStage;
    }

    public Player getPlayer() {
        return player;
    }

    public int getOldStage() {
        return oldStage;
    }

    public int getNewStage() {
        return newStage;
    }
}
