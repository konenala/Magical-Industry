package com.github.nalamodikk.common.network.handler;

import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MagicalIndustryMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ServerNetworkHandler.registerPackets(); // 註冊伺服器端的封包
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientNetworkHandler.registerPackets(); // 註冊客戶端的封包
            }
        });
    }
}

