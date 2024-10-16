package com.github.nalamodikk.item.debug;

import com.github.nalamodikk.block.entity.ManaCraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ManaDebugToolItem extends Item {

    public ManaDebugToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ManaCraftingTableBlockEntity manaBlockEntity) {
                // 增加魔力值以调试
                manaBlockEntity.addMana(10);
                System.out.println("Debug: Added 10 Mana. Current Mana: " + manaBlockEntity.getManaStored());

                // 给玩家显示消息
                Player player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.magical_industry.mana_added", manaBlockEntity.getManaStored()), true);
                }

                // 确保状态已更新
                manaBlockEntity.setChanged();
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 3);

                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }
}