package com.crystaltech.capability;

/**
 * Small immutable contract describing the player's CrystalTech stage.
 */
public interface ICrystalStage {
    int MIN_STAGE = 0;
    int MAX_STAGE = 3;

    int getStage();

    void setStage(int stage);

    default boolean tryAdvanceTo(int targetStage) {
        if (targetStage <= getStage() || targetStage < MIN_STAGE || targetStage > MAX_STAGE) {
            return false;
        }
        setStage(targetStage);
        return true;
    }
}
