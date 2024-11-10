package com.github.nalamodikk.common.network.toolpacket;

import com.github.nalamodikk.common.Capability.ManaCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
        NetworkEvent.Context ctx = context.get();

        // 根據目標側判斷是否需要在伺服器或客戶端處理
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> {
                // 確保只在客戶端執行
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Level level = Minecraft.getInstance().level;
                    if (level != null) {
                        BlockEntity blockEntity = level.getBlockEntity(msg.pos);
                        if (blockEntity != null) {
                            blockEntity.getCapability(ManaCapability.MANA).ifPresent(manaStorage -> {
                                manaStorage.setMana(msg.mana);
                            });
                        }
                    }
                });
            });
        }

        ctx.setPacketHandled(true);
    }

}
