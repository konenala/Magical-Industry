package com.github.nalamodikk.common.network;


import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.ToggleModePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MagicalIndustryMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerPackets() {
        NETWORK_CHANNEL.registerMessage(0, ToggleModePacket.class, ToggleModePacket::encode, ToggleModePacket::decode, ToggleModePacket::handle);
    }

    public static void init(final FMLCommonSetupEvent event) {
        registerPackets();
    }
}