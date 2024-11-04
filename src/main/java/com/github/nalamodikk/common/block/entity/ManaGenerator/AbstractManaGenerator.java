package com.github.nalamodikk.common.block.entity.ManaGenerator;

import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.compat.energy.NalaEnergyStorage;
import com.github.nalamodikk.common.util.loader.FuelRateLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractManaGenerator extends BlockEntity {

    protected final EnergyStorage energyStorage; // 使用 Forge 自帶的能量存儲系統
    protected final ManaStorage manaStorage; // 自定義魔力存儲
    protected int burnTime; // 燃燒時間（適用於魔力和能量模式）
    protected int productionRate; // 生成速率（魔力或能量）
    protected boolean isWorking; // 表示設備是否在工作
    protected Mode currentMode; // 當前的運行模式（魔力或能量）
    protected final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }
    };

    public abstract ContainerData getContainerData();

    public enum Mode {
        MANA, ENERGY
    }

    public AbstractManaGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyCapacity, int manaCapacity) {
        super(type, pos, state);

        this.energyStorage = new NalaEnergyStorage(energyCapacity);
        this.manaStorage = new ManaStorage(manaCapacity);
        this.burnTime = 0;
        this.productionRate = 0;
        this.currentMode = Mode.MANA; // 默認為魔力模式
    }

    protected void loadFuel() {
        if (hasFuel()) {
            ItemStack fuel = fuelHandler.getStackInSlot(0);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(fuel.getItem());
            FuelRateLoader.FuelRate fuelRate = FuelRateLoader.getFuelRateForItem(itemId);

            if (currentMode == Mode.MANA) {
                if (fuelRate != null && fuelRate.getBurnTime() > 0) {
                    burnTime = fuelRate.getBurnTime();
                    productionRate = fuelRate.getManaRate();
                    fuelHandler.extractItem(0, 1, false);
                    isWorking = true;
                }
            } else if (currentMode == Mode.ENERGY) {
                if (fuelRate != null && fuelRate.getBurnTime() > 0) {
                    burnTime = fuelRate.getBurnTime();
                    productionRate = fuelRate.getEnergyRate();
                } else {
                    int forgeBurnTime = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
                    if (forgeBurnTime > 0) {
                        burnTime = forgeBurnTime;
                        productionRate = calculateEnergyProductionRate(fuel);
                    }
                }

                if (burnTime > 0) {
                    fuelHandler.extractItem(0, 1, false);
                    isWorking = true;
                }
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractManaGenerator blockEntity) {
        if (!level.isClientSide) {
            blockEntity.commonTick();
        }
    }

    protected void commonTick() {
        if (!isWorking && hasFuel()) {
            loadFuel(); // 加載燃料
        }

        if (burnTime > 0) {
            burnTime--;
            if (burnTime == 0) {
                // 燃燒完成，生成魔力或能量
                if (currentMode == Mode.MANA) {
                    manaStorage.addMana(productionRate);
                } else if (currentMode == Mode.ENERGY) {
                    energyStorage.receiveEnergy(productionRate, false);
                }
                isWorking = false; // 完成生成
            }
        }

        markUpdated(); // 更新狀態
    }

    protected int calculateEnergyProductionRate(ItemStack fuel) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(fuel.getItem());
        FuelRateLoader.FuelRate fuelRate = FuelRateLoader.getFuelRateForItem(itemId);
        return fuelRate != null ? fuelRate.getEnergyRate() : 0;
    }

    public abstract void drops();

    public boolean hasFuel() {
        return !fuelHandler.getStackInSlot(0).isEmpty();
    }

    public void markUpdated() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
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
}
