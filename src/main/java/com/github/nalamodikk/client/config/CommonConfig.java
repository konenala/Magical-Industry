package com.github.nalamodikk.client.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    public final ForgeConfigSpec.IntValue ENERGY_RATE;
    public final ForgeConfigSpec.IntValue MAX_ENERGY;

    public CommonConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Mana Generator Settings").push("mana_generator");

        ENERGY_RATE = builder
                .comment("能量生成速率 (每秒)")
                .defineInRange("energyRate", 10, 1, Integer.MAX_VALUE);

        MAX_ENERGY = builder
                .comment("最大能量存儲")
                .defineInRange("maxEnergy", 10000, 1, Integer.MAX_VALUE);

        builder.pop();
    }
}
