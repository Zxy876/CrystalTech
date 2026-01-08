package com.crystaltech.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.IntTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Supplies the crystal stage capability for player entities.
 */
public class CrystalStageProvider implements ICapabilityProvider, ICapabilitySerializable<IntTag> {
    private final CrystalStage instance = new CrystalStage();
    private final LazyOptional<ICrystalStage> optional = LazyOptional.of(() -> instance);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        return capability == CrystalStageCapability.CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public IntTag serializeNBT() {
        return instance.serialize();
    }

    @Override
    public void deserializeNBT(IntTag nbt) {
        instance.deserialize(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
