package com.github.nalamodikk.common.register;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ConfigManager {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new CommonConfig(commonBuilder);
        COMMON_SPEC = commonBuilder.build();
    }

    public static void registerConfigs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "ManaGeneratorBlock-common.toml");
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.IntValue energyRate;
        public final ForgeConfigSpec.IntValue maxEnergy;
        public final ForgeConfigSpec.IntValue defaultBurnTime;
        public final ForgeConfigSpec.IntValue defaultEnergyRate;
        public final ForgeConfigSpec.IntValue defaultManaRate;
        public final ForgeConfigSpec.IntValue manaRate;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("mana_generator");

            energyRate = builder
                    .comment("能量生成速率 (每秒)")
                    .defineInRange("energyRate", 1, 1, Integer.MAX_VALUE);

            maxEnergy = builder
                    .comment("最大能量存儲")
                    .defineInRange("maxEnergy", 10000, 1, Integer.MAX_VALUE);

            defaultBurnTime = builder
                    .comment("默認燃燒時間（適用於無特定配置的物品）")
                    .defineInRange("defaultBurnTime", 200, 1, Integer.MAX_VALUE);

            defaultEnergyRate = builder
                    .comment("默認能量生成速率（適用於無特定配置的物品）")
                    .defineInRange("defaultEnergyRate", 1, 1, Integer.MAX_VALUE);

            defaultManaRate = builder
                    .comment("默認魔力生成速率（適用於無特定配置的物品）")
                    .defineInRange("defaultManaRate", 5, 1, Integer.MAX_VALUE);


            manaRate = builder
                    .comment("魔力生成速率 (每秒)")
                    .defineInRange("manaRate", 5, 1, Integer.MAX_VALUE);

            builder.pop();
        }
    }
}
