package com.github.nalamodikk.common.network;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.blockentity.ManaGenerator.ManaGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleModePacket {
    private final BlockPos pos;

    public ToggleModePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ToggleModePacket msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.pos);
    }

    public static ToggleModePacket decode(FriendlyByteBuf buffer) {
        return new ToggleModePacket(buffer.readBlockPos());
    }

    public static void handle(ToggleModePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // 获取方块实体
                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);
                if (blockEntity instanceof ManaGeneratorBlockEntity manaGenerator) {
                    // 检查工作状态
                    if (manaGenerator.getWorkingStatus()) {
                        player.displayClientMessage(Component.translatable("message.magical_industry.cannot_toggle_while_working"), true);
                    } else {
                        manaGenerator.toggleMode();
                        manaGenerator.setChanged(); // 标记数据变化
                    }
                } else {
                    MagicalIndustryMod.LOGGER.warn("Failed to toggle mode: BlockEntity at {} is not a ManaGeneratorBlockEntity.", packet.pos);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
    public BlockPos getPos() {
        return pos;
    }


}
