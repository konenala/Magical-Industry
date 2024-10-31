package com.github.nalamodikk.common.block.entity.ManaGenerator;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractManaGenerator extends BlockEntity {

    protected final EnergyStorage energyStorage; // 使用Forge自帶的能量存儲系統
    protected int burnTime; // 當前燃燒時間
    protected int currentBurnTime; // 總燃燒時間
    protected int energyProductionRate; // 能量生產速率

    protected AbstractManaGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyCapacity) {
        super(type, pos, state);
        this.energyStorage = new EnergyStorage(energyCapacity);
        this.energyProductionRate = 0;
    }

    // 基本能量生成的方法
    public void produceEnergy() {
        if (burnTime > 0) {
            burnTime--;
            energyStorage.receiveEnergy(energyProductionRate, false); // 生成能量並存入能量儲存
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return LazyOptional.of(() -> energyStorage).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.burnTime = tag.getInt("BurnTime");
        this.currentBurnTime = tag.getInt("CurrentBurnTime");
        this.energyProductionRate = tag.getInt("EnergyProductionRate");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("CurrentBurnTime", this.currentBurnTime);
        tag.putInt("EnergyProductionRate", this.energyProductionRate);
    }

    public int getEnergyProductionRate() {
        return energyProductionRate;
    }

    public void setEnergyProductionRate(int rate) {
        this.energyProductionRate = rate;
    }
}
