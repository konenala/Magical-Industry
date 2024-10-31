package com.github.nalamodikk.common.Capability;
public interface IMana {
    int getMana();
    void addMana(int amount);
    void consumeMana(int amount);
    void setMana(int amount);

    // 新增此方法用來標記 Capability 的變化
    void onChanged();

    int getMaxMana();
}
