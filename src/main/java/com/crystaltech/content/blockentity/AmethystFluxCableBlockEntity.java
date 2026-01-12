package com.crystaltech.content.blockentity;

import javax.annotation.Nullable;

import com.crystaltech.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Simple FE cable that balances energy with adjacent storages.
 */
public class AmethystFluxCableBlockEntity extends BlockEntity {
    public static final int CAPACITY = 2000;
    public static final int MAX_TRANSFER = 200;

    private final FluxEnergyStorage energyStorage = new FluxEnergyStorage(CAPACITY, MAX_TRANSFER);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);

    public AmethystFluxCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMETHYST_FLUX_CABLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmethystFluxCableBlockEntity cable) {
        cable.pullEnergy();
        cable.pushEnergy();
    }

    private void pullEnergy() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (Direction direction : Direction.values()) {
            NeighborAccess neighbor = getNeighbor(direction);
            if (neighbor == null) {
                continue;
            }
            if (!neighbor.storage().canExtract()) {
                continue;
            }
            if (neighbor.entity() instanceof AmethystFluxCableBlockEntity cable) {
                if (cable.getStoredEnergy() <= getStoredEnergy()) {
                    continue;
                }
            }
            int space = energyStorage.getSpace();
            if (space <= 0) {
                return;
            }
            int extracted = neighbor.storage().extractEnergy(Math.min(MAX_TRANSFER, space), false);
            if (extracted > 0) {
                energyStorage.receiveEnergy(extracted, false);
                setChanged();
            }
        }
    }

    private void pushEnergy() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (energyStorage.getEnergyStored() <= 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (energyStorage.getEnergyStored() <= 0) {
                return;
            }
            NeighborAccess neighbor = getNeighbor(direction);
            if (neighbor == null) {
                continue;
            }
            if (!neighbor.storage().canReceive()) {
                continue;
            }
            if (neighbor.entity() instanceof AmethystFluxCableBlockEntity cable) {
                if (cable.getStoredEnergy() >= getStoredEnergy()) {
                    continue;
                }
            }
            int available = Math.min(MAX_TRANSFER, energyStorage.getEnergyStored());
            int accepted = neighbor.storage().receiveEnergy(available, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
                setChanged();
            }
        }
    }

    @Nullable
    private NeighborAccess getNeighbor(Direction direction) {
        if (level == null) {
            return null;
        }
        BlockPos neighborPos = worldPosition.relative(direction);
        BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
        if (neighborEntity == null) {
            return null;
        }
        IEnergyStorage storage = neighborEntity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
        if (storage == null) {
            return null;
        }
        return new NeighborAccess(neighborEntity, storage);
    }

    public void addEnergy(int amount) {
        energyStorage.receiveEnergy(amount, false);
        setChanged();
    }

    public int getStoredEnergy() {
        return energyStorage.getEnergyStored();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energyStorage.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergy(tag.getInt("Energy"));
    }

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }

    private class FluxEnergyStorage extends EnergyStorage {
        private FluxEnergyStorage(int capacity, int maxTransfer) {
            super(capacity, maxTransfer, maxTransfer);
        }

        private int getSpace() {
            return getMaxEnergyStored() - getEnergyStored();
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                onChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                onChanged();
            }
            return extracted;
        }

        private void onChanged() {
            AmethystFluxCableBlockEntity.this.setChanged();
        }

        private void setEnergy(int amount) {
            this.energy = Math.max(0, Math.min(amount, getMaxEnergyStored()));
        }
    }

    private record NeighborAccess(BlockEntity entity, IEnergyStorage storage) {
    }
}
