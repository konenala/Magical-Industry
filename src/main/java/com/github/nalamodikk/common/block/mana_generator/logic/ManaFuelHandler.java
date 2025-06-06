package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.common.block.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.block.mana_generator.recipe.loader.ManaGenFuelRateLoader.FuelRate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ManaFuelHandler {
    private final ManaGeneratorStateManager  stateManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(ManaFuelHandler.class);

    private final ItemStackHandler fuelHandler;
    private ResourceLocation currentFuelId;
    private int burnTime;
    private int currentBurnTime;
    private int failedFuelCooldown;
    private boolean isPaused;        // 是否暫停（因產出空間滿了）
    private boolean needsFuel = true;
    private boolean recoveryAttempted = false;



    public ManaFuelHandler( ItemStackHandler fuelHandler,ManaGeneratorStateManager stateManager) {
        this.fuelHandler = fuelHandler;
        this.stateManager = stateManager;

    }


    public boolean hasAttemptedRecovery() {
        return recoveryAttempted;
    }

    public void markRecoveryAttempted() {
        this.recoveryAttempted = true;
    }

    public void resetRecoveryFlag() {
        this.recoveryAttempted = false;
    }


    public void tickCooldown() {
        if (failedFuelCooldown > 0) {
            failedFuelCooldown--;
            if (failedFuelCooldown == 0) {
                markNeedsFuel(); // cooldown 結束再嘗試消耗
            }
        }
    }

    public boolean tryConsumeFuel() {
        if (!needsFuel || burnTime > 0 || failedFuelCooldown > 0) {
            return false;
        }

        ItemStack fuel = fuelHandler.getStackInSlot(0);
        if (fuel.isEmpty() || fuel.getItem() == null) return false; // ← 加這裡保險

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(fuel.getItem());
        FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(id);

        if (rate == null || rate.getBurnTime() <= 0) {
            failedFuelCooldown = 20; // cooldown 20 tick 再試
            needsFuel = false;       // 暫時不要再試，等待 cooldown
            return false;
        }

        switch (stateManager.getCurrentMode()) {
            case MANA -> {
                if (rate.getManaRate() <= 0) {
                    failedFuelCooldown = 20;
                    needsFuel = false;
                    return false;
                }
            }
            case ENERGY -> {
                if (rate.getEnergyRate() <= 0) {
                    failedFuelCooldown = 20;
                    needsFuel = false;
                    return false;
                }
            }
        }

        currentFuelId = id;
        currentBurnTime = rate.getBurnTime();
        burnTime = currentBurnTime;
        fuelHandler.extractItem(0, 1, false);
        needsFuel = false; // 燃料已成功啟動，不需再嘗試
        recoveryAttempted = false;

        return true;


    }

    public void markNeedsFuel() {
        this.needsFuel = true;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getCurrentBurnTime() {
        return currentBurnTime;
    }

    public ResourceLocation getCurrentFuelId() {
        return currentFuelId;
    }

    public boolean isCoolingDown() {
        return failedFuelCooldown > 0;
    }


    public void setCooldown() {
        this.failedFuelCooldown = 20;
    }



    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }


    public void setCurrentFuelId(@Nullable ResourceLocation id) {
        this.currentFuelId = id;
    }


    public void resetBurnTime() {
        this.burnTime = 0;
        this.currentBurnTime = 0;
    }

    public void tickBurn(boolean allowBurn) {
        if (!allowBurn || isPaused) return;

        if (burnTime > 0) {
            burnTime--;
            if (burnTime == 0) {
                currentBurnTime  = 0;
            }
        }
    }

    public void pauseBurn() {
        if (!isPaused) {
            isPaused = true;
            failedFuelCooldown = Math.max(failedFuelCooldown, 20); // 最少 1 秒後會重試
            LOGGER.debug("pauseBurn(): machine paused, cooldown = {}", failedFuelCooldown);
        }
    }


    public void resumeBurn() {
        if (isPaused) {
            this.isPaused = false;
            LOGGER.debug("resumeBurn(): machine resumed from pause");
        }
    }

    public boolean isPaused() {
        return isPaused;
    }
    public float getFuelProgress() {
        if (currentBurnTime == 0) return 0f;
        return burnTime / (float) currentBurnTime;
    }


    public boolean isBurning() {
        return burnTime > 0;
    }

    public Optional<ManaGenFuelRateLoader.FuelRate> getCurrentFuelRate() {
        if (currentFuelId == null) return Optional.empty();

        ManaGenFuelRateLoader.FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(currentFuelId);
        if (rate == null) return Optional.empty();

        // ✅ 根據模式篩選產能是否合法
        switch (stateManager.getCurrentMode()) {
            case MANA -> {
                if (rate.getManaRate() <= 0) return Optional.empty();
            }
            case ENERGY -> {
                if (rate.getEnergyRate() <= 0) return Optional.empty();
            }
        }

        return Optional.of(rate);
    }


}
