package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.config.ManaConduitConfig;
import com.github.nalamodikk.common.config.ManaConduitConfigLoader;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class ConfigManager {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new CommonConfig(commonBuilder);
        COMMON_SPEC = commonBuilder.build();
    }

    public static void registerConfigs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "magical_industry/ManaGeneratorBlock-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ManaConduitConfig.SPEC, "magical_industry/ManaConduit-common.toml");
    }

    // ✅ **正確讀取設定值的時機：當 Forge 完成設定載入後**
    @SubscribeEvent
    public static void onLoadComplete(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ManaConduitConfig.SPEC) {
            MagicalIndustryMod.LOGGER.info("ManaConduitConfig Loaded! Transfer Rate: " + ManaConduitConfig.getManaConduitTransferRate());
        }
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
                    .comment("能量生成速率 (每tick)")
                    .defineInRange("energyRate", 1, 0, Integer.MAX_VALUE);

            maxEnergy = builder
                    .comment("最大能量存儲")
                    .defineInRange("maxEnergy", 10000, 1, Integer.MAX_VALUE);

            defaultBurnTime = builder
                    .comment("默認燃燒時間（適用於無特定配置的物品）")
                    .defineInRange("defaultBurnTime", 200, 0, Integer.MAX_VALUE);

            defaultEnergyRate = builder
                    .comment("默認能量生成速率（適用於無特定配置的物品）")
                    .defineInRange("defaultEnergyRate", 1, 0, Integer.MAX_VALUE);

            defaultManaRate = builder
                    .comment("默認魔力生成速率（適用於無特定配置的物品）")
                    .defineInRange("defaultManaRate", 5, 0, Integer.MAX_VALUE);


            manaRate = builder
                    .comment("魔力生成速率 (每tick)")
                    .defineInRange("manaRate", 5, 0, Integer.MAX_VALUE);

            builder.pop();
        }
    }
}
