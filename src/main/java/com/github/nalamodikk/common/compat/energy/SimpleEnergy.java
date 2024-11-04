package com.github.nalamodikk.common.compat.energy;

import net.minecraftforge.energy.IEnergyStorage;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SimpleEnergy implements IEnergyStorage {

    private static final int DECIMAL_DIGITS = 4;
    private long value; // 整數部分
    private short decimal; // 小數部分
    private final int capacity; // 最大能量容量

    public static final SimpleEnergy ZERO = new SimpleEnergy(0);
    public static final SimpleEnergy MAX_VALUE = new SimpleEnergy(Long.MAX_VALUE);

    public SimpleEnergy(long value) {
        this.value = Math.max(0, value);
        this.decimal = 0;
        this.capacity = Integer.MAX_VALUE; // 默认无限制
    }

    public SimpleEnergy(long value, short decimal) {
        this.value = Math.max(0, value);
        this.decimal = clampDecimal(decimal);
        this.capacity = Integer.MAX_VALUE; // 默认无限制
    }

    public SimpleEnergy(long value, int capacity) {
        this.value = Math.min(value, capacity);
        this.decimal = 0;
        this.capacity = capacity;
    }

    // 对接 Forge 的能量系统，提供能量容量设置
    public SimpleEnergy(long value, short decimal, int capacity) {
        this.value = Math.min(value, capacity);
        this.decimal = clampDecimal(decimal);
        this.capacity = capacity;
    }

    public static SimpleEnergy of(double value) {
        long lValue = (long) value;
        short decimal = parseDecimal(BigDecimal.valueOf(value).setScale(DECIMAL_DIGITS, RoundingMode.DOWN));
        return new SimpleEnergy(lValue, decimal);
    }

    private static short parseDecimal(BigDecimal decimalValue) {
        String[] parts = decimalValue.toPlainString().split("\\.");
        if (parts.length < 2) {
            return 0;
        }
        String decimalString = parts[1];
        return Short.parseShort(decimalString.length() > DECIMAL_DIGITS ? decimalString.substring(0, DECIMAL_DIGITS) : decimalString);
    }

    // Forge IEnergyStorage methods implementation
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) return 0;

        SimpleEnergy energyToAdd = SimpleEnergy.of(maxReceive);
        SimpleEnergy remainingCapacity = SimpleEnergy.of(capacity).subtract(this);

        SimpleEnergy acceptedEnergy = energyToAdd.greaterOrEqual(remainingCapacity) ? remainingCapacity : energyToAdd;

        if (!simulate) {
            this.plusEqual(acceptedEnergy);
            ensureWithinCapacity();
        }

        return (int) acceptedEnergy.getTotalEnergy();
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0) return 0;

        SimpleEnergy energyToExtract = SimpleEnergy.of(maxExtract);
        SimpleEnergy energyAvailable = SimpleEnergy.of(this.value).add(SimpleEnergy.of(this.decimal / 10000.0));

        SimpleEnergy extractedEnergy = energyToExtract.greaterOrEqual(energyAvailable) ? energyAvailable : energyToExtract;

        if (!simulate) {
            this.minusEqual(extractedEnergy);
        }

        return (int) extractedEnergy.getTotalEnergy();
    }

    @Override
    public int getEnergyStored() {
        return (int) this.getTotalEnergy();
    }

    @Override
    public int getMaxEnergyStored() {
        return this.capacity;
    }

    @Override
    public boolean canExtract() {
        return !this.isZero();
    }

    @Override
    public boolean canReceive() {
        return this.getEnergyStored() < this.getMaxEnergyStored();
    }

    // 现有的方法保持不变
    private static short clampDecimal(short decimal) {
        return (short) Math.min(Math.max(0, decimal), 9999);
    }

    public double getTotalEnergy() {
        BigDecimal totalValue = new BigDecimal(this.value).add(new BigDecimal(this.decimal).movePointLeft(DECIMAL_DIGITS));
        return totalValue.doubleValue();
    }

    // 其他现有方法保持不变
    public SimpleEnergy add(SimpleEnergy toAdd) {
        long newValue = this.value + toAdd.value;
        short newDecimal = (short) (this.decimal + toAdd.decimal);
        if (newDecimal >= 10000) {
            newValue++;
            newDecimal -= 10000;
        }
        return new SimpleEnergy(newValue, newDecimal, this.capacity);
    }

    public SimpleEnergy subtract(SimpleEnergy toSubtract) {
        if (this.isZero() || toSubtract.isZero() || toSubtract.greaterOrEqual(this)) {
            return ZERO;
        }
        long newValue = this.value - toSubtract.value;
        short newDecimal = (short) (this.decimal - toSubtract.decimal);
        if (newDecimal < 0) {
            newValue--;
            newDecimal += 10000;
        }
        return new SimpleEnergy(newValue, newDecimal, this.capacity);
    }

    public boolean isZero() {
        return this.value == 0 && this.decimal == 0;
    }

    public boolean greaterOrEqual(SimpleEnergy other) {
        return this.value > other.value || (this.value == other.value && this.decimal >= other.decimal);
    }

    public SimpleEnergy copy() {
        return new SimpleEnergy(this.value, this.decimal, this.capacity);
    }

    public SimpleEnergy multiply(int factor) {
        BigDecimal totalValue = new BigDecimal(this.value).add(new BigDecimal(this.decimal).movePointLeft(DECIMAL_DIGITS));
        BigDecimal result = totalValue.multiply(BigDecimal.valueOf(factor)).setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        return SimpleEnergy.of(result.doubleValue());
    }

    public SimpleEnergy divide(int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }
        BigDecimal totalValue = new BigDecimal(this.value).add(new BigDecimal(this.decimal).movePointLeft(DECIMAL_DIGITS));
        BigDecimal result = totalValue.divide(BigDecimal.valueOf(divisor), DECIMAL_DIGITS, RoundingMode.DOWN);
        return SimpleEnergy.of(result.doubleValue());
    }

    public SimpleEnergy plusEqual(SimpleEnergy toAdd) {
        this.value += toAdd.value;
        this.decimal += toAdd.decimal;
        if (this.decimal >= 10000) {
            this.value++;
            this.decimal -= 10000;
        }
        ensureWithinCapacity();
        return this;
    }

    public SimpleEnergy minusEqual(SimpleEnergy toSubtract) {
        if (toSubtract.greaterOrEqual(this)) {
            this.value = 0;
            this.decimal = 0;
        } else {
            this.value -= toSubtract.value;
            this.decimal -= toSubtract.decimal;
            if (this.decimal < 0) {
                this.value--;
                this.decimal += 10000;
            }
        }
        return this;
    }

    public SimpleEnergy generatePerSecond(int ratePerSecond, int seconds) {
        // 計算指定秒數內的能量生成
        BigDecimal totalEnergy = BigDecimal.valueOf(ratePerSecond).multiply(BigDecimal.valueOf(seconds));
        return SimpleEnergy.of(totalEnergy.doubleValue());
    }

    public SimpleEnergy multiply(SimpleEnergy other) {
        BigDecimal totalValue = new BigDecimal(this.value).add(new BigDecimal(this.decimal).movePointLeft(DECIMAL_DIGITS));
        BigDecimal otherValue = new BigDecimal(other.value).add(new BigDecimal(other.decimal).movePointLeft(DECIMAL_DIGITS));
        BigDecimal result = totalValue.multiply(otherValue).setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        return SimpleEnergy.of(result.doubleValue());
    }

    // 确保能量值不会超出容量
    private void ensureWithinCapacity() {
        if (this.getTotalEnergy() > this.capacity) {
            this.value = this.capacity;
            this.decimal = 0;
        }
    }


}
