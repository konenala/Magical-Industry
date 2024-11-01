package com.github.nalamodikk.client.model;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.ManaGenerator.ManaGeneratorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ManaGeneratorModel extends GeoModel<ManaGeneratorBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ManaGeneratorBlockEntity animatable) {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "geo/mana_generator.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ManaGeneratorBlockEntity animatable) {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/block/mana_generator_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ManaGeneratorBlockEntity animatable) {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "animations/mana_generator.animation.json");
    }
}
