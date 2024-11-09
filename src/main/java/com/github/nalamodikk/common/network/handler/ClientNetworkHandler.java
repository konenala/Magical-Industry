package com.github.nalamodikk.common.network.handler;

import com.github.nalamodikk.common.network.ManaUpdatePacket;
import com.github.nalamodikk.common.network.PacketIDHelper;
import com.github.nalamodikk.common.network.toolpacket.TechWandModePacket;

public class ClientNetworkHandler {
    public static void registerPackets() {
        // 客戶端專用的封包註冊
        NetworkHandler.NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), ManaUpdatePacket.class, ManaUpdatePacket::encode, ManaUpdatePacket::decode, ManaUpdatePacket::handle);
        NetworkHandler.NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), TechWandModePacket.class, TechWandModePacket::encode, TechWandModePacket::decode, TechWandModePacket::handle);
    }
}
