package com.github.nalamodikk.common.network;

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

    public static void encode(TechWandModePacket msg, FriendlyByteBuf buffer) {
        buffer.writeBoolean(msg.forward);
    }

    public static TechWandModePacket decode(FriendlyByteBuf buffer) {
        return new TechWandModePacket(buffer.readBoolean());
    }

    public static void handle(TechWandModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (stack.getItem() instanceof BasicTechWandItem) {
                    BasicTechWandItem wand = (BasicTechWandItem) stack.getItem();
                    // 根據滾輪方向切換模式
                    if (msg.forward) {
                        wand.setMode(stack, wand.getMode(stack).next());
                    } else {
                        wand.setMode(stack, wand.getMode(stack).previous());
                    }
                    // 顯示本地化信息給玩家
                    player.displayClientMessage(
                            Component.translatable("message.magical_industry.mode_changed",
                                    Component.translatable("mode.magical_industry." + wand.getMode(stack).name().toLowerCase())),
                            true
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
