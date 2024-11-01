package com.github.nalamodikk.common.mana;

import net.minecraftforge.energy.EnergyStorage;

public class NalaEnergyStorage extends EnergyStorage {
    public NalaEnergyStorage(int capacity) {
        super(capacity);
    }

    // 自定義方法來檢查是否已滿
    public boolean isFull() {
        return this.getEnergyStored() >= this.getMaxEnergyStored();
    }
}
