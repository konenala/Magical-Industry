package com.github.nalamodikk.common.compat.energy;

import net.minecraftforge.energy.IEnergyStorage;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class UnifiedEnergyStorage implements IEnergyStorage {
    private BigDecimal energy; // 用於同時表示整數和小數部分
    private final BigDecimal maxCapacity;
    private final int DECIMAL_DIGITS = 4;

    // 使用三種不同類型的構造函數進行初始化
    public UnifiedEnergyStorage(int capacity) {
        this.energy = BigDecimal.ZERO.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        this.maxCapacity = BigDecimal.valueOf(capacity);
    }

    public UnifiedEnergyStorage(long capacity) {
        this.energy = BigDecimal.ZERO.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        this.maxCapacity = BigDecimal.valueOf(capacity);
    }

    public UnifiedEnergyStorage(BigInteger capacity) {
        this.energy = BigDecimal.ZERO.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        this.maxCapacity = new BigDecimal(capacity);
    }

    public void setEnergy(BigDecimal value) {
        // 確保能量不會超過最大容量，也不會低於 0
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            energy = BigDecimal.ZERO;
        } else if (value.compareTo(maxCapacity) > 0) {
            energy = maxCapacity;
        } else {
            energy = value.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        }
    }

    // 接收能量方法，支援 int、long、BigInteger
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0 || !canReceive()) {
            return 0;
        }

        // 首先進行模擬操作來確認可以接收的能量量
        BigDecimal energyToAdd = BigDecimal.valueOf(maxReceive);
        BigDecimal acceptedEnergy = energyToAdd.min(maxCapacity.subtract(energy));

        // 如果 simulate 為 false，則進行實際的儲存
        if (!simulate && acceptedEnergy.compareTo(BigDecimal.ZERO) > 0) {
            energy = energy.add(acceptedEnergy);
        }

        // 返回這次（無論是模擬或真實）能夠接受的能量量
        return acceptedEnergy.intValue();
    }



    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (maxReceive <= 0 || !canReceive()) {
            return 0;
        }
        BigDecimal energyToAdd = BigDecimal.valueOf(maxReceive);
        BigDecimal acceptedEnergy = energyToAdd.min(maxCapacity.subtract(energy));

        if (!simulate && acceptedEnergy.compareTo(BigDecimal.ZERO) > 0) {
            energy = energy.add(acceptedEnergy);
        }

        return acceptedEnergy.longValue();
    }

    public BigInteger receiveEnergy(BigInteger maxReceive, boolean simulate) {
        if (maxReceive.compareTo(BigInteger.ZERO) <= 0 || !canReceive()) {
            return BigInteger.ZERO;
        }
        BigDecimal energyToAdd = new BigDecimal(maxReceive);
        BigDecimal acceptedEnergy = energyToAdd.min(maxCapacity.subtract(energy));

        if (!simulate && acceptedEnergy.compareTo(BigDecimal.ZERO) > 0) {
            energy = energy.add(acceptedEnergy);
        }

        return acceptedEnergy.toBigInteger();
    }

    // 提取能量方法，支援 int、long、BigInteger
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0 || !canExtract()) {
            return 0;
        }
        BigDecimal energyToExtract = BigDecimal.valueOf(maxExtract);
        BigDecimal extractedEnergy = energy.min(energyToExtract);

        if (!simulate && extractedEnergy.compareTo(BigDecimal.ZERO) > 0) {
            energy = energy.subtract(extractedEnergy);
        }

        return extractedEnergy.intValue();
    }

    public long extractEnergy(long maxExtract, boolean simulate) {
        if (maxExtract <= 0 || !canExtract()) {
            return 0;
        }
        BigDecimal energyToExtract = BigDecimal.valueOf(maxExtract);
        BigDecimal extractedEnergy = energy.min(energyToExtract);

        if (!simulate && extractedEnergy.compareTo(BigDecimal.ZERO) > 0) {
            energy = energy.subtract(extractedEnergy);
        }

        return extractedEnergy.longValue();
    }

    public BigInteger extractEnergy(BigInteger maxExtract, boolean simulate) {
        if (maxExtract.compareTo(BigInteger.ZERO) <= 0 || !canExtract()) {
            return BigInteger.ZERO;
        }
        BigDecimal energyToExtract = new BigDecimal(maxExtract);
        BigDecimal extractedEnergy = energy.min(energyToExtract);

        if (!simulate && extractedEnergy.compareTo(BigDecimal.ZERO) > 0) {
            energy = energy.subtract(extractedEnergy);
        }

        return extractedEnergy.toBigInteger();
    }

    // 獲取當前能量值，支援 int、long、BigInteger
    @Override
    public int getEnergyStored() {
        return energy.intValue();
    }

    public long getEnergyStoredLong() {
        return energy.longValue();
    }

    public BigInteger getEnergyStoredBigInt() {
        return energy.toBigInteger();
    }

    // 獲取最大能量容量，支援 int、long、BigInteger
    @Override
    public int getMaxEnergyStored() {
        return maxCapacity.intValue();
    }

    public long getMaxEnergyStoredLong() {
        return maxCapacity.longValue();
    }

    public BigInteger getMaxEnergyStoredBigInt() {
        return maxCapacity.toBigInteger();
    }

    @Override
    public boolean canExtract() {
        return energy.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean canReceive() {
        return energy.compareTo(maxCapacity) < 0;
    }

    // 新增的高精度操作方法
    public BigDecimal getEnergy() {
        return energy;
    }

    public void addEnergy(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            energy = energy.add(amount).min(maxCapacity);
        }
    }

    public void subtractEnergy(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            energy = energy.subtract(amount).max(BigDecimal.ZERO);
        }
    }

    // 判斷能量是否已滿或為零
    public boolean isFull() {
        return energy.compareTo(maxCapacity) >= 0;
    }

    public boolean isEmpty() {
        return energy.compareTo(BigDecimal.ZERO) == 0;
    }

}
