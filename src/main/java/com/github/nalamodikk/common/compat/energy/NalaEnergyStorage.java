package com.github.nalamodikk.common.compat.energy;

import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import java.math.BigInteger;

public class NalaEnergyStorage extends EnergyStorage implements IEnergyStorage {
    private BigInteger maxEnergyBigInt;
    private BigInteger currentEnergyBigInt;
    private long maxEnergyLong;
    private long currentEnergyLong;
    private boolean useBigInteger;
    private boolean useLong;

    // Constructor for int capacity
    public NalaEnergyStorage(int capacity) {
        super(capacity);
        this.useBigInteger = false;
        this.useLong = false;
    }

    // Constructor for long capacity
    public NalaEnergyStorage(long capacity) {
        super(capacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity);
        if (capacity > Integer.MAX_VALUE) {
            this.maxEnergyLong = capacity;
            this.currentEnergyLong = 0L;
            this.useLong = true;
        } else {
            this.useLong = false;
        }
        this.useBigInteger = false;
    }

    // Constructor for BigInteger capacity
    public NalaEnergyStorage(BigInteger maxEnergy) {
        super(maxEnergy.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ? Integer.MAX_VALUE : maxEnergy.intValue());
        this.maxEnergyBigInt = maxEnergy;
        this.currentEnergyBigInt = BigInteger.ZERO;
        this.useBigInteger = true;
        this.useLong = false;
    }

    // Override to set energy (supports int, long, and BigInteger)
    public void setEnergy(int energy) {
        if (useBigInteger) {
            setEnergy(BigInteger.valueOf(energy));
        } else if (useLong) {
            setEnergy((long) energy);
        } else {
            this.energy = Math.min(energy, getMaxEnergyStored());
        }
    }

    public void setEnergy(long energy) {
        if (useBigInteger) {
            setEnergy(BigInteger.valueOf(energy));
        } else if (useLong) {
            this.currentEnergyLong = Math.min(energy, maxEnergyLong);
        } else {
            throw new UnsupportedOperationException("Current storage is using int type. Use setEnergy(int) instead.");
        }
    }

    public void setEnergy(BigInteger energy) {
        if (useBigInteger) {
            if (energy.compareTo(maxEnergyBigInt) > 0) {
                this.currentEnergyBigInt = maxEnergyBigInt;
            } else if (energy.compareTo(BigInteger.ZERO) < 0) {
                this.currentEnergyBigInt = BigInteger.ZERO;
            } else {
                this.currentEnergyBigInt = energy;
            }
        } else {
            throw new UnsupportedOperationException("Current storage is using int or long type. Use setEnergy(int/long) instead.");
        }
    }

    // Add energy (supports int, long, and BigInteger)
    public void addEnergy(int energy) {
        if (useBigInteger) {
            addEnergy(BigInteger.valueOf(energy));
        } else if (useLong) {
            addEnergy((long) energy);
        } else {
            setEnergy(getEnergyStored() + energy);
        }
    }

    public void addEnergy(long energy) {
        if (useBigInteger) {
            addEnergy(BigInteger.valueOf(energy));
        } else if (useLong) {
            setEnergy(currentEnergyLong + energy);
        } else {
            throw new UnsupportedOperationException("Current storage is using int type. Use addEnergy(int) instead.");
        }
    }

    public void addEnergy(BigInteger energy) {
        if (useBigInteger) {
            setEnergy(currentEnergyBigInt.add(energy));
        } else {
            throw new UnsupportedOperationException("Current storage is using int or long type. Use addEnergy(int/long) instead.");
        }
    }

    // Extract energy (supports int, long, and BigInteger)
    public int extractEnergy(int amount, boolean simulate) {
        if (useBigInteger) {
            throw new UnsupportedOperationException("Use extractEnergy(BigInteger, boolean) for BigInteger storage.");
        } else if (useLong) {
            throw new UnsupportedOperationException("Use extractEnergy(long, boolean) for long storage.");
        }
        return super.extractEnergy(amount, simulate);
    }

    public long extractEnergy(long amount, boolean simulate) {
        if (useBigInteger) {
            throw new UnsupportedOperationException("Use extractEnergy(BigInteger, boolean) for BigInteger storage.");
        } else if (useLong) {
            long extracted = Math.min(amount, currentEnergyLong);
            if (!simulate) {
                currentEnergyLong -= extracted;
            }
            return extracted;
        } else {
            throw new UnsupportedOperationException("Current storage is using int type. Use extractEnergy(int, boolean) instead.");
        }
    }

    public BigInteger extractEnergy(BigInteger amount, boolean simulate) {
        if (useBigInteger) {
            if (amount.compareTo(currentEnergyBigInt) > 0) {
                amount = currentEnergyBigInt;
            }

            if (!simulate) {
                currentEnergyBigInt = currentEnergyBigInt.subtract(amount);
            }

            return amount;
        } else {
            throw new UnsupportedOperationException("Current storage is using int or long type. Use extractEnergy(int/long, boolean) instead.");
        }
    }

    // Check if storage is full (supports all types)
    public boolean isFull() {
        if (useBigInteger) {
            return currentEnergyBigInt.compareTo(maxEnergyBigInt) >= 0;
        } else if (useLong) {
            return currentEnergyLong >= maxEnergyLong;
        } else {
            return getEnergyStored() >= getMaxEnergyStored();
        }
    }

    // Check if storage is empty (supports all types)
    public boolean isEmpty() {
        if (useBigInteger) {
            return currentEnergyBigInt.equals(BigInteger.ZERO);
        } else if (useLong) {
            return currentEnergyLong == 0;
        } else {
            return getEnergyStored() == 0;
        }
    }

    // Get current energy (supports all types)
    public BigInteger getCurrentEnergy() {
        if (useBigInteger) {
            return currentEnergyBigInt;
        } else if (useLong) {
            return BigInteger.valueOf(currentEnergyLong);
        } else {
            return BigInteger.valueOf(getEnergyStored());
        }
    }

    // Get max energy (supports all types)
    public BigInteger getMaxEnergy() {
        if (useBigInteger) {
            return maxEnergyBigInt;
        } else if (useLong) {
            return BigInteger.valueOf(maxEnergyLong);
        } else {
            return BigInteger.valueOf(getMaxEnergyStored());
        }
    }

    // Receive energy (supports SimpleEnergy)
    public SimpleEnergy receiveEnergyAsSimple(int amount, boolean simulate) {
        if (useBigInteger) {
            BigInteger received = receiveEnergy(BigInteger.valueOf(amount), simulate);
            return new SimpleEnergy(received.intValue());
        } else if (useLong) {
            long received = extractEnergy(amount, simulate);
            return new SimpleEnergy((int) received);
        } else {
            int received = receiveEnergy(amount, simulate);
            return new SimpleEnergy(received);
        }
    }

    // 接收能量的方法（支持 BigInteger）
    private BigInteger receiveEnergy(BigInteger bigInteger, boolean simulate) {
        // 計算可以接收的能量量
        BigInteger availableSpace = maxEnergyBigInt.subtract(currentEnergyBigInt);
        BigInteger energyToReceive = bigInteger.min(availableSpace);

        // 如果不是模擬操作，就更新當前能量
        if (!simulate) {
            currentEnergyBigInt = currentEnergyBigInt.add(energyToReceive);
        }

        // 返回實際接收到的能量
        return energyToReceive;
    }


    public SimpleEnergy receiveEnergy(SimpleEnergy energy, boolean simulate) {
        int amount = (int) energy.getTotalEnergy();
        return receiveEnergyAsSimple(amount, simulate);
    }
}
