package com.github.nalamodikk.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ManaConduitConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ManaConduitConfig INSTANCE;

    private final ForgeConfigSpec.IntValue manaConduitTransferRate;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        INSTANCE = new ManaConduitConfig(builder);
        SPEC = builder.build();
    }

    private ManaConduitConfig(ForgeConfigSpec.Builder builder) {
        builder.push("mana_conduit");

        manaConduitTransferRate = builder
                .comment("魔力導管每 tick 最大傳輸速率")
                .defineInRange("manaConduitTransferRate", 50, 1, Integer.MAX_VALUE);


        builder.pop();
    }

    /**
     * 獲取魔力導管的傳輸速率
     * @return 每 tick 允許的最大魔力傳輸速率
     */
    public static int getManaConduitTransferRate() {
        return INSTANCE.manaConduitTransferRate.get();
    }



    /**
     * 兼容舊代碼，提供與 getManaConduitTransferRate 相同的功能
     * @return 預設魔力導管傳輸速率
     */
    public static int getDefaultManaTransferRate() {
        return getManaConduitTransferRate();
    }
}
