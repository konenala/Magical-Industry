package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.common.NeoMagnaMod;
import com.github.nalamodikk.common.block.block.ManaGenerator.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.blockentity.ManaGenerator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.client.model.ManaGeneratorModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ManaGeneratorRenderer extends GeoBlockRenderer<ManaGeneratorBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(NeoMagnaMod.MOD_ID, "textures/block/mana_generator_texture.png");
    private static final ResourceLocation WORKING_TEXTURE = new ResourceLocation(NeoMagnaMod.MOD_ID, "textures/block/mana_generator_working.png");

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

    @Override
    public void preRender(PoseStack poseStack, ManaGeneratorBlockEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // 在渲染之前處理自定義邏輯，例如添加發光效果
        if (animatable.getBlockState().getValue(ManaGeneratorBlock.WORKING)) {
            // 添加發光邏輯
            boolean isWorking = animatable.getBlockState().getValue(ManaGeneratorBlock.WORKING);
            int lightLevel = isWorking ? 15728880 : packedLight;

        }

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

}
