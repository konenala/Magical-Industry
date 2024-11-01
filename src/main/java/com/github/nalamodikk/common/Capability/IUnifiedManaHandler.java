package com.github.nalamodikk.common.Capability;

import com.github.nalamodikk.common.mana.ManaAction;

// 新的接口，整合了 ISimpleManaHandler 和 IStrictManaHandler
public interface IUnifiedManaHandler {

    // 單一槽位管理
    int getMana();
    void addMana(int amount);
    void consumeMana(int amount);
    void setMana(int amount);
    void onChanged();
    int getMaxMana();

    default int getNeeded() {
        return Math.max(0, getMaxMana() - getMana());
    }

    default int insertMana(int amount, ManaAction action) {
        if (amount <= 0) {
            return 0;
        }
        int needed = getNeeded();
        if (needed == 0) {
            return amount; // 容器已滿，返回全部未能插入的量
        }
        int toAdd = Math.min(amount, needed);
        if (action.execute()) {
            addMana(toAdd);
            onChanged(); // 插入成功後通知變化
        }
        return amount - toAdd;
    }

    default int extractMana(int amount, ManaAction action) {
        if (amount <= 0 || getMana() == 0) {
            return 0;
        }
        int extracted = Math.min(amount, getMana());
        if (action.execute()) {
            consumeMana(extracted);
            onChanged(); // 提取成功後通知變化
        }
        return extracted;
    }

    // 多槽位管理
    int getManaContainerCount();

    int getMana(int container);

    void setMana(int container, int mana);

    int getMaxMana(int container);

    int getNeededMana(int container);

    int insertMana(int container, int amount, ManaAction action);

    int extractMana(int container, int amount, ManaAction action);

    // 插入到多個槽位
    default int insertManaToAllContainers(int amount, ManaAction action) {
        int remaining = amount;
        for (int i = 0; i < getManaContainerCount(); i++) {
            remaining = insertMana(i, remaining, action);
            if (remaining == 0) {
                break;
            }
        }
        return remaining;
    }

    // 從多個槽位提取
    default int extractManaFromAllContainers(int amount, ManaAction action) {
        int extracted = 0;
        for (int i = 0; i < getManaContainerCount(); i++) {
            extracted += extractMana(i, amount - extracted, action);
            if (extracted >= amount) {
                break;
            }
        }
        return extracted;
    }
}
