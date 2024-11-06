package com.github.nalamodikk.common.network;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConfigDirectionUpdatePacket {
    private final BlockPos blockPos;
    private final Direction direction;
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

    public static void handle(ConfigDirectionUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                BlockEntity blockEntity = level.getBlockEntity(msg.blockPos);

                if (blockEntity instanceof IConfigurableBlock configurableBlock) {
                    configurableBlock.setDirectionConfig(msg.direction, msg.isOutput);
                    level.sendBlockUpdated(msg.blockPos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

