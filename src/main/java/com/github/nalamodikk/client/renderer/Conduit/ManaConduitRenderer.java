package com.github.nalamodikk.client.renderer.Conduit;

import com.github.nalamodikk.common.block.block.Conduit.ManaConduitBlock;
import com.github.nalamodikk.common.block.blockentity.Conduit.ManaConduitBlockEntity;
import com.github.nalamodikk.common.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;

public class ManaConduitRenderer implements BlockEntityRenderer<ManaConduitBlockEntity> {
    private static final ResourceLocation CONDUIT_TEXTURE = new ResourceLocation("magical_industry", "textures/block/mana_conduit.png");

    public ManaConduitRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ManaConduitBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 使用單一貼圖渲染整個形狀
        RenderUtils.renderShape(
                poseStack,
                bufferSource.getBuffer(RenderType.entityCutout(CONDUIT_TEXTURE)),
                blockEntity.getCachedShape(blockEntity.getBlockState()), // 獲取完整的形狀
                packedLight,
                packedOverlay

        );

    }

}
