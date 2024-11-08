package com.github.nalamodikk.common.network;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketHandler {

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
