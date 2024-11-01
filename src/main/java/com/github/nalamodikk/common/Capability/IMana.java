package com.github.nalamodikk.common.Capability;
import com.github.nalamodikk.common.mana.ManaAction;

public interface IMana {
    int getMana();
    void addMana(int amount);
    void consumeMana(int amount);
    void setMana(int amount);

    // 新增此方法用來標記 Capability 的變化
    void onChanged();

    int getMaxMana();

    // 新增：獲取所需的魔力
    default int getNeeded() {
        return Math.max(0, getMaxMana() - getMana());
    }

    // 新增：插入魔力的方法
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

    // 新增：提取魔力的方法
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
}
