package com.crystaltech.capability.intent;

import java.time.Instant;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Supplies the player intent capability for player entities.
 */
public class PlayerIntentProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
    private final PlayerIntent instance = new PlayerIntent();
    private final LazyOptional<IPlayerIntent> optional = LazyOptional.of(() -> instance);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        return capability == PlayerIntentCapability.CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return instance.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserialize(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }

    public void markStageConsumed(int stageReached) {
        instance.markConsumed(stageReached, Instant.now());
    }
}
