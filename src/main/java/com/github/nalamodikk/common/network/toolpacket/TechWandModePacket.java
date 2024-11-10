package com.github.nalamodikk.common.network.toolpacket;

import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TechWandModePacket {
    private final boolean forward;

    public TechWandModePacket(boolean forward) {
        this.forward = forward;
    }

    // 編碼和解碼方法保持不變
    public static void encode(TechWandModePacket msg, FriendlyByteBuf buffer) {
        buffer.writeBoolean(msg.forward);
    }

    public static TechWandModePacket decode(FriendlyByteBuf buffer) {
        return new TechWandModePacket(buffer.readBoolean());
    }

    // 封包處理方法
    public static void handle(TechWandModePacket msg, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.getSender(); // 確保這裡獲取的是伺服器端的玩家
            if (serverPlayer != null) {
                ItemStack stack = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                if (stack.getItem() instanceof BasicTechWandItem) {
                    BasicTechWandItem wand = (BasicTechWandItem) stack.getItem();
                    BasicTechWandItem.TechWandMode currentMode = wand.getMode(stack);
                    BasicTechWandItem.TechWandMode newMode = msg.forward ? currentMode.next() : currentMode.previous();
                    wand.setMode(stack, newMode);

                    // 向玩家顯示切換模式的信息
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.magical_industry.mode_changed",
                            Component.translatable("mode.magical_industry." + newMode.name().toLowerCase())), true);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
