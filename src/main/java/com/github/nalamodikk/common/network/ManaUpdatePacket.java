package com.github.nalamodikk.common.network;

import com.github.nalamodikk.common.Capability.ManaCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ManaUpdatePacket {
    private final BlockPos pos;
    private final int mana;

    public ManaUpdatePacket(BlockPos pos, int mana) {
        this.pos = pos;
        this.mana = mana;
    }

    // 封包的編碼方法，用於發送數據
    public static void encode(ManaUpdatePacket msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.pos);
        buffer.writeInt(msg.mana);
    }

    // 封包的解碼方法，用於接收數據
    public static ManaUpdatePacket decode(FriendlyByteBuf buffer) {
        return new ManaUpdatePacket(buffer.readBlockPos(), buffer.readInt());
    }

    // 處理封包的方法，當封包到達客戶端時的操作
    public static void handle(ManaUpdatePacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                BlockEntity blockEntity = level.getBlockEntity(msg.pos);
                if (blockEntity != null) {
                    // 不指定具體的方塊類型，改為檢查是否有 ManaCapability
                    blockEntity.getCapability(ManaCapability.MANA).ifPresent(manaStorage -> {
                        manaStorage.setMana(msg.mana); // 使用魔力能力來更新魔力值
                    });
                }
            }
        });
        context.get().setPacketHandled(true);
    }

}
