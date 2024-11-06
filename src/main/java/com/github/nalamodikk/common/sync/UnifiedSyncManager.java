package com.github.nalamodikk.common.sync;

import net.minecraft.world.inventory.ContainerData;

public class UnifiedSyncManager {
    private final ContainerData containerData;

    public UnifiedSyncManager(int dataCount) {
        containerData = new ContainerData() {
            private final int[] data = new int[dataCount];

            @Override
            public int get(int index) {
                if (index >= 0 && index < data.length) {
                    return data[index];
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < data.length) {
                    data[index] = value;
                }
            }

            @Override
            public int getCount() {
                return data.length;
            }
        };
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public void set(int index, int value) {
        containerData.set(index, value);
    }

    public int get(int index) {
        return containerData.get(index);
    }
}
