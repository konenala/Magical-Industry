package com.github.nalamodikk.client.model;

import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.;

public class UnifiedModel<T extends BlockEntity & IAnimatable> extends AnimatedGeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T entity) {
        // 根據不同的類型返回對應的模型文件
        if (entity instanceof ManaGeneratorBlockEntity) {
            return new ResourceLocation(MagicalIndustryMod.MOD_ID, "geo/mana_generator.geo.json");
        }
        // 其他實體類型的模型資源
        if (entity instanceof AnotherBlockEntity) {
            return new ResourceLocation(MagicalIndustryMod.MOD_ID, "geo/another_block.geo.json");
        }
        // 默認返回空模型
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "geo/default_model.geo.json");
    }

   /* @Override
    public ResourceLocation getTextureResource(T entity) {
        // 根據不同的類型返回對應的材質文件
        if (entity instanceof ManaGeneratorBlockEntity) {
            return new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/block/mana_generator.png");
        }
        if (entity instanceof AnotherBlockEntity) {
            return new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/block/another_block.png");
        }
        // 默認返回空材質
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/block/default_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T entity) {
        // 根據不同的類型返回對應的動畫文件
        if (entity instanceof ManaGeneratorBlockEntity) {
            return new ResourceLocation(MagicalIndustryMod.MOD_ID, "animations/mana_generator.animation.json");
        }
        if (entity instanceof AnotherBlockEntity) {
            return new ResourceLocation(MagicalIndustryMod.MOD_ID, "animations/another_block.animation.json");
        }
        // 默認返回空動畫
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "animations/default_animation.json");
    }

    */
}

