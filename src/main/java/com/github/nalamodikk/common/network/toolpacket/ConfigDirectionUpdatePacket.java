package com.github.nalamodikk.common.network.toolpacket;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConfigDirectionUpdatePacket {
    private final Direction direction;
    private final BlockPos blockPos;
    ;
    private final boolean isOutput;

    public ConfigDirectionUpdatePacket(BlockPos blockPos, Direction direction, boolean isOutput) {
        this.blockPos = blockPos;
        this.direction = direction;
        this.isOutput = isOutput;
    }

    public static void encode(ConfigDirectionUpdatePacket msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.blockPos);
        buffer.writeEnum(msg.direction);
        buffer.writeBoolean(msg.isOutput);
    }

    public static ConfigDirectionUpdatePacket decode(FriendlyByteBuf buffer) {
        return new ConfigDirectionUpdatePacket(buffer.readBlockPos(), buffer.readEnum(Direction.class), buffer.readBoolean());
    }

    public BlockPos getPos() {
        return this.blockPos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public boolean isOutput() {
        return this.isOutput;
    }


    public static void handle(ConfigDirectionUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;  // 確保玩家不為空

            ServerLevel level = player.serverLevel();
            BlockPos pos = packet.getPos();
            Direction direction = packet.getDirection();

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IConfigurableBlock configurableBlock) {
                configurableBlock.setDirectionConfig(direction, packet.isOutput());
                blockEntity.setChanged(); // 標記狀態已更改以便保存到伺服器

                // 向所有玩家同步方塊狀態更新
                level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);

                // 日誌打印，用於檢查方向變更是否成功
                MagicalIndustryMod.LOGGER.info("Player {} set Direction {} to {} for block at {}",
                        player.getName().getString(), direction, packet.isOutput() ? "Output" : "Input", pos);
            } else {
                // 如果 blockEntity 不是可配置的方塊，記錄錯誤信息
                MagicalIndustryMod.LOGGER.error("Failed to set direction for block at {} because it is not an instance of IConfigurableBlock.", pos);
            }
        });

        context.setPacketHandled(true);
    }


}
