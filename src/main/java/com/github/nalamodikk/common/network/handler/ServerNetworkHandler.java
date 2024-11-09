package com.github.nalamodikk.common.network.handler;

import com.github.nalamodikk.common.network.PacketIDHelper;
import com.github.nalamodikk.common.network.ToggleModePacket;
import com.github.nalamodikk.common.network.toolpacket.ConfigDirectionUpdatePacket;

public class ServerNetworkHandler {
    public static void registerPackets() {
        // 伺服器專用的封包註冊
        NetworkHandler.NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), ToggleModePacket.class, ToggleModePacket::encode, ToggleModePacket::decode, ToggleModePacket::handle);
        NetworkHandler.NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), ConfigDirectionUpdatePacket.class, ConfigDirectionUpdatePacket::encode, ConfigDirectionUpdatePacket::decode, ConfigDirectionUpdatePacket::handle);
        NetworkHandler.NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), ConfigDirectionUpdatePacket.class, ConfigDirectionUpdatePacket::encode, ConfigDirectionUpdatePacket::decode, PacketHandler::handleConfigDirectionUpdate);


    }
}
