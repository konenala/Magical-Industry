package com.github.nalamodikk.common.network;

import com.github.nalamodikk.common.block.entity.ManaGenerator.ManaGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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

    public static void handle(ToggleModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
//                MagicalIndustryMod.LOGGER.info("Handling toggle mode packet on server for position: " + msg.pos);
                ServerLevel level = (ServerLevel) player.getCommandSenderWorld();

                BlockEntity blockEntity = level.getBlockEntity(msg.pos);
                if (blockEntity instanceof ManaGeneratorBlockEntity manaGenerator) {
                    manaGenerator.toggleMode(); // 切換模式
                    level.sendBlockUpdated(msg.pos, manaGenerator.getBlockState(), manaGenerator.getBlockState(), 3);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }





}
