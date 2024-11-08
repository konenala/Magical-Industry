package com.github.nalamodikk.common.register;


import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorMenu;
import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorScreen;
import com.github.nalamodikk.common.screen.manacrafting.ManaCraftingScreen;
import com.github.nalamodikk.common.screen.ModMenusTypes;
import com.github.nalamodikk.common.screen.tool.UniversalConfigMenu;
import com.github.nalamodikk.common.screen.tool.UniversalConfigScreen;
import net.minecraft.client.gui.screens.MenuScreens;

public class ModMenuScreens {
    public static void registerScreens() {
        // 註冊菜單屏幕
        MenuScreens.register(ModMenusTypes.MANA_CRAFTING_MENU.get(), ManaCraftingScreen::new);
        MenuScreens.register(ModMenusTypes.MANA_GENERATOR_MENU.get(), ManaGeneratorScreen::new);
        MenuScreens.register(ModMenusTypes.UNIVERSAL_CONFIG.get(), UniversalConfigScreen::new);


    }
}
