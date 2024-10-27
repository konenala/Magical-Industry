package com.github.nalamodikk.client.model;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.custom.blockitem.ManaGeneratorBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ManaGeneratorBlockItemModel extends GeoModel<ManaGeneratorBlockItem> {
    @Override
    public ResourceLocation getModelResource(ManaGeneratorBlockItem animatable) {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "geo/mana_generator.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ManaGeneratorBlockItem animatable) {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/block/mana_generator_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ManaGeneratorBlockItem animatable) {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "animations/mana_generator.animation.json");
    }
}
