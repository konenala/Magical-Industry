package com.github.nalamodikk.common.API;

public final class MagicNBTConstants {

    private MagicNBTConstants() {
    }

    // 與魔力相關的 NBT 常數
    public static final String MANA_STORED = "ManaStored"; // 當前儲存的魔力量
    public static final String MAX_MANA_CAPACITY = "MaxManaCapacity"; // 最大魔力儲存容量
    public static final String MANA_PRODUCTION_RATE = "ManaProductionRate"; // 每次燃燒時的魔力生成速率
    public static final String MANA_CONSUMPTION_RATE = "ManaConsumptionRate"; // 魔力消耗速率，用於某些特定操作或持續消耗
    public static final String BURN_TIME_MANA = "BurnTimeMana"; // 當前燃料的魔力燃燒時間
    public static final String CURRENT_MODE = "CurrentMode"; // 當前模式，例如 0 代表魔力模式，1 代表能量模式
    public static final String IS_WORKING = "IsWorking"; // 發電機當前是否在運作中
    public static final String FUEL_TYPE = "FuelType"; // 當前燃燒的燃料類型標識符
    public static final String FUEL_REMAINING = "FuelRemaining"; // 當前燃料還剩餘多少可燃燒
    public static final String MANA_GENERATOR_LEVEL = "ManaGeneratorLevel"; // 發電機的等級或升級狀態，影響其性能
    public static final String LAST_FUEL_ITEM = "LastFuelItem"; // 最後一個被使用的燃料物品，用於在燃料切換時處理
    public static final String OWNER_UUID = "OwnerUUID"; // 擁有者的 UUID，用於多人世界中管理擁有權
    public static final String TEMPERATURE = "Temperature"; // 當前的內部溫度（如果需要影響生成速率）
    public static final String ENERGY_LINKED = "EnergyLinked"; // 是否連接到其他能量設備或網路（可用於互動效果）

    // 通用與能量/魔力混合系統的常數
    public static final String ENERGY_STORED = "EnergyStored"; // 當前儲存的能量
    public static final String MAX_ENERGY_CAPACITY = "MaxEnergyCapacity"; // 最大能量儲存容量
    public static final String ENERGY_PRODUCTION_RATE = "EnergyProductionRate"; // 能量生成速率
    public static final String BURN_TIME_ENERGY = "BurnTimeEnergy"; // 當前燃料的能量燃燒時間
    public static final String MANA_TO_ENERGY_CONVERSION_RATE = "ManaToEnergyConversionRate"; // 魔力轉換為能量的比率（用於多模式機器）

}
