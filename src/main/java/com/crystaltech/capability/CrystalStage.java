package com.crystaltech.capability;

import net.minecraft.nbt.IntTag;

/**
 * Mutable implementation that persists the crystal stage as an integer.
 */
public final class CrystalStage implements ICrystalStage {
    private int stage = MIN_STAGE;

    @Override
    public int getStage() {
        return stage;
    }

    @Override
    public void setStage(int stage) {
        this.stage = clamp(stage);
    }

    public IntTag serialize() {
        return IntTag.valueOf(stage);
    }

    public void deserialize(IntTag tag) {
        setStage(tag.getAsInt());
    }

    private static int clamp(int value) {
        if (value < MIN_STAGE) {
            return MIN_STAGE;
        }
        if (value > MAX_STAGE) {
            return MAX_STAGE;
        }
        return value;
    }
}
