package com.github.nalamodikk.client.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    public final ForgeConfigSpec.IntValue ENERGY_RATE;

    public CommonConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Mana Generator Settings").push("mana_generator");

        ENERGY_RATE = builder
                .comment("能量生成速率 (每秒)")
                .defineInRange("energyRate", 10, 1, Integer.MAX_VALUE);

        builder.pop();
    }
}
