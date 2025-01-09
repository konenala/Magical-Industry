package com.github.nalamodikk.common.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ManaConduitModelProvider extends BlockModelProvider {

    public ManaConduitModelProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, "magical_industry", fileHelper);
    }

    @Override
    protected void registerModels() {
        // 為每個方向生成模型
        singleTexture("mana_conduit_center",
                new ResourceLocation("minecraft", "block/block"),
                "all",
                new ResourceLocation("magical_industry", "block/conduit/mana_conduit_center"));

        singleTexture("mana_conduit_north",
                new ResourceLocation("minecraft", "block/block"),
                "all",
                new ResourceLocation("magical_industry", "block/conduit/mana_conduit_north"));

        // 重複同樣的方式為其他方向生成
        singleTexture("mana_conduit_east",
                new ResourceLocation("minecraft", "block/block"),
                "all",
                new ResourceLocation("magical_industry", "block/conduit/mana_conduit_east"));
        singleTexture("mana_conduit_south",
                new ResourceLocation("minecraft", "block/block"),
                "all",
                new ResourceLocation("magical_industry", "block/conduit/mana_conduit_south"));
        singleTexture("mana_conduit_west",
                new ResourceLocation("minecraft", "block/block"),
                "all",
                new ResourceLocation("magical_industry", "block/conduit/mana_conduit_west"));
        singleTexture("mana_conduit_up",
                new ResourceLocation("minecraft", "block/block"),
                "all",
                new ResourceLocation("magical_industry", "block/conduit/mana_conduit_up"));
        singleTexture("mana_conduit_down",
                new ResourceLocation("minecraft", "block/block"),
                "all",
                new ResourceLocation("magical_industry", "block/conduit/mana_conduit_down"));
    }
}
