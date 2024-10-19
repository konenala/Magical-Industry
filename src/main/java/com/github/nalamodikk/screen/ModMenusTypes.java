package com.github.nalamodikk.screen;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.block.entity.mana_crafting_table.AdvancedManaCraftingTableBlockEntity;
import com.github.nalamodikk.block.entity.mana_crafting_table.BaseManaCraftingTableBlockEntity;
import com.github.nalamodikk.screen.mana_crafting_table.AdvancedManaCraftingTableMenu;
import com.github.nalamodikk.screen.mana_crafting_table.BaseManaCraftingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenusTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MagicalIndustryMod.MOD_ID);

    public static final RegistryObject<MenuType<BaseManaCraftingMenu>> BASE_MANA_CRAFTING_MENU = MENUS.register("mana_crafting", () -> IForgeMenuType.create((windowId, inv, data) -> {
        Level level = inv.player.getCommandSenderWorld(); // 获取玩家所在的世界
        BlockPos pos = data.readBlockPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BaseManaCraftingTableBlockEntity) {
            return new BaseManaCraftingMenu(ModMenusTypes.ADVANCED_MANA_CRAFTING_TABLE_MENU.get(), windowId, inv, ((BaseManaCraftingTableBlockEntity) blockEntity).getItemHandler(), ContainerLevelAccess.create(level, pos), level, (BaseManaCraftingTableBlockEntity) blockEntity);
        }
        throw new IllegalStateException("Block entity is not correct!");
    }));

    public static final RegistryObject<MenuType<AdvancedManaCraftingTableMenu>> ADVANCED_MANA_CRAFTING_TABLE_MENU = MENUS.register("advanced_mana_crafting", () -> IForgeMenuType.create((windowId, inv, data) -> {
        Level level = inv.player.getCommandSenderWorld(); // 获取玩家所在的世界
        BlockPos pos = data.readBlockPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AdvancedManaCraftingTableBlockEntity) {
            return new AdvancedManaCraftingTableMenu(windowId, inv, (AdvancedManaCraftingTableBlockEntity) blockEntity);
        }
        throw new IllegalStateException("Block entity is not correct!");
    }));


    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
