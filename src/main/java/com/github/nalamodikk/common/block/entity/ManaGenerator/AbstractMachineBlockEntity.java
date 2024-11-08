package com.github.nalamodikk.common.block.entity;

import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.compat.energy.UnifiedEnergyStorage;
import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public abstract class AbstractMachineBlockEntity extends BlockEntity {
    protected final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);

    protected final UnifiedEnergyStorage energyStorage;
    protected final ManaStorage manaStorage;
    protected boolean supportsEnergy;
    protected boolean supportsMana;

    protected int burnTime;
    protected int currentBurnTime;

    private final LazyOptional<UnifiedEnergyStorage> lazyEnergyStorage;
    private final LazyOptional<ManaStorage> lazyManaStorage;

    public AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean supportsEnergy, boolean supportsMana) {
        super(type, pos, state);

        for (Direction direction : Direction.values()) {
            directionConfig.put(direction, false);
        }

        this.supportsEnergy = supportsEnergy;
        this.supportsMana = supportsMana;

        this.energyStorage = supportsEnergy ? new UnifiedEnergyStorage(getConfigMaxEnergy()) : null;
        this.manaStorage = supportsMana ? new ManaStorage(getConfigMaxMana()) : null;

        this.lazyEnergyStorage = supportsEnergy ? LazyOptional.of(() -> energyStorage) : LazyOptional.empty();
        this.lazyManaStorage = supportsMana ? LazyOptional.of(() -> manaStorage) : LazyOptional.empty();
    }


    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
        markUpdated();
    }

    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }

    public static int getConfigMaxEnergy() {
        // 從配置文件中獲取最大能量值
        return 10000;
    }

    public static int getConfigMaxMana() {
        // 從配置文件中獲取最大魔力值
        return 10000;
    }

    public void markUpdated() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractMachineBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.serverTick();
        }
    }

    protected void serverTick() {
        if (supportsEnergy) {
            generateEnergy();
        }
        if (supportsMana) {
            generateMana();
        }
        outputEnergyAndMana();
        markUpdated();
    }

    protected abstract void generateEnergy();

    protected abstract void generateMana();

    private void outputEnergyAndMana() {
        for (Direction direction : Direction.values()) {
            BlockEntity neighborBlockEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (neighborBlockEntity != null) {
                if (supportsEnergy) {
                    neighborBlockEntity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(neighborEnergyStorage -> {
                        if (neighborEnergyStorage.canReceive()) {
                            int energyToTransfer = Math.min(energyStorage.getEnergyStored(), 100);
                            int acceptedEnergy = neighborEnergyStorage.receiveEnergy(energyToTransfer, false);
                            energyStorage.extractEnergy(acceptedEnergy, false);
                        }
                    });
                }
                if (supportsMana) {
                    neighborBlockEntity.getCapability(ManaCapability.MANA, direction.getOpposite()).ifPresent(neighborManaStorage -> {
                        if (neighborManaStorage.canReceive()) {
                            int manaToTransfer = Math.min(manaStorage.getMana(), 50);
                            int acceptedMana = neighborManaStorage.receiveMana(manaToTransfer, ManaAction.get(false));
                            manaStorage.extractMana(acceptedMana, ManaAction.get(false));
                        }
                    });
                }
            }
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && supportsEnergy) {
            return lazyEnergyStorage.cast();
        } else if (cap == ManaCapability.MANA && supportsMana) {
            return lazyManaStorage.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (supportsMana && tag.contains("ManaStored")) {
            manaStorage.setMana(tag.getInt("ManaStored"));
        }
        if (supportsEnergy && tag.contains("EnergyStored")) {
            energyStorage.receiveEnergy(tag.getInt("EnergyStored"), false);
        }
        burnTime = tag.getInt("BurnTime");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (supportsMana) {
            tag.putInt("ManaStored", manaStorage.getMana());
        }
        if (supportsEnergy) {
            tag.putInt("EnergyStored", energyStorage.getEnergyStored());
        }
        tag.putInt("BurnTime", burnTime);
    }
}
