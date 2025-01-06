package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.block.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.blockentity.ManaGenerator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.client.model.ManaGeneratorModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ManaGeneratorRenderer extends GeoBlockRenderer<ManaGeneratorBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/block/mana_generator_texture.png");
    private static final ResourceLocation WORKING_TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/block/mana_generator_working.png");

    public ManaGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(new ManaGeneratorModel());
    }

    @Override
    public ResourceLocation getTextureLocation(ManaGeneratorBlockEntity animatable) {
        // 根據工作狀態返回整個方塊的貼圖
        if (animatable.getBlockState().getValue(ManaGeneratorBlock.WORKING)) {
            return WORKING_TEXTURE;
        } else {
            return TEXTURE;
        }
    }

}
