package com.github.nalamodikk.common.sync;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;

import java.util.ArrayList;
import java.util.List;

// 同步管理器類，負責多個機器的同步
public class MachineSyncManager implements ContainerData {
    private final List<DataSlot> dataSlots = new ArrayList<>();

    // 添加一個 DataSlot
    public void addDataSlot(DataSlot dataSlot) {
        dataSlots.add(dataSlot);
    }

    @Override
    public int get(int index) {
        return dataSlots.get(index).get();
    }

    @Override
    public void set(int index, int value) {
        dataSlots.get(index).set(value);
    }

    @Override
    public int getCount() {
        return dataSlots.size();
    }

    // 工具方法，用於添加不同類型的數據
    public void addEnergySlot(int[] energyValueHolder) {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return energyValueHolder[0];
            }

            @Override
            public void set(int value) {
                energyValueHolder[0] = value;
            }
        });
    }

    public void addManaSlot(int[] manaValueHolder) {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return manaValueHolder[0];
            }

            @Override
            public void set(int value) {
                manaValueHolder[0] = value;
            }
        });
    }
}
