package com.github.nalamodikk.screen;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.block.entity.ManaCraftingTableBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.network.IContainerFactory;

public class ModMenusTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MagicalIndustryMod.MOD_ID);

    public static final RegistryObject<MenuType<ManaCraftingMenu>> MANA_CRAFTING_MENU = MENUS.register("mana_crafting", () -> IForgeMenuType.create((windowId, inv, data) -> {
        Level level = inv.player.getCommandSenderWorld(); // 获取玩家所在的世界
        return new ManaCraftingMenu(windowId, inv, new ItemStackHandler(10), ContainerLevelAccess.NULL, level);
    }));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
