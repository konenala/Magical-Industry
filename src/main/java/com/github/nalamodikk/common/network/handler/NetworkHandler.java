package com.github.nalamodikk.common.network.handler;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.PacketIDHelper;
import com.github.nalamodikk.common.network.ToggleModePacket;
import com.github.nalamodikk.common.network.toolpacket.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.toolpacket.ManaUpdatePacket;
import com.github.nalamodikk.common.network.toolpacket.ModeChangePacket;
import com.github.nalamodikk.common.network.toolpacket.TechWandModePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MagicalIndustryMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );


    public static void init(final FMLCommonSetupEvent event) {
        // 使用 FMLCommonSetupEvent 進行封包的統一註冊
        // 統一註冊所有封包
        event.enqueueWork(NetworkHandler::registerPackets);
    }

    public static void registerPackets() {
        // 使用 PacketIDHelper 生成唯一的封包 ID

        // 註冊所有封包
        NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), ToggleModePacket.class,
                ToggleModePacket::encode, ToggleModePacket::decode, ToggleModePacket::handle);

        NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), ConfigDirectionUpdatePacket.class,
                ConfigDirectionUpdatePacket::encode, ConfigDirectionUpdatePacket::decode, PacketHandler::handleConfigDirectionUpdate);

        // 客戶端專用封包註冊
        NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), ModeChangePacket.class,
                ModeChangePacket::toBytes, ModeChangePacket::new, ModeChangePacket::handle);

        NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), TechWandModePacket.class,
                TechWandModePacket::encode, TechWandModePacket::decode, TechWandModePacket::handle);

        NETWORK_CHANNEL.registerMessage(PacketIDHelper.getNextId(), ManaUpdatePacket.class,
                ManaUpdatePacket::encode, ManaUpdatePacket::decode, ManaUpdatePacket::handle);
    }


    public static class PacketHandler {

        // 伺服器端處理封包的方法
        public static void handleConfigDirectionUpdate(ConfigDirectionUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer player = context.getSender();
            if (player == null) return;

            BlockPos pos = packet.getPos();
            Direction direction = packet.getDirection();

            context.enqueueWork(() -> {
                Level level = player.level();
                if (level != null) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof IConfigurableBlock configurableBlock) {
                        boolean isCurrentlyOutput = configurableBlock.isOutput(direction);
                        configurableBlock.setDirectionConfig(direction, !isCurrentlyOutput);
                        blockEntity.setChanged(); // 標記狀態已更改以保存到伺服器

                        // 向所有玩家同步方塊更新
                        level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
                    }
                }
            });

            context.setPacketHandled(true);
        }

    }

}