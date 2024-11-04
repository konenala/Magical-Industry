package com.github.nalamodikk.client.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public final ForgeConfigSpec.BooleanValue ENABLE_PARTICLES;

    public ClientConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Client Settings").push("client");

        ENABLE_PARTICLES = builder
                .comment("是否啟用粒子效果")
                .define("enableParticles", true);

        builder.pop();
    }
}
