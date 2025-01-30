package com.github.nalamodikk.common.util;

import com.github.nalamodikk.common.block.blockentity.Conduit.ManaConduitBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RenderUtils {

    /**
     * 將 VoxelShape 渲染到對應的位置。
     *
     * @param poseStack      當前的 PoseStack。
     * @param vertexConsumer 目標 VertexConsumer。
     * @param shape          要渲染的 VoxelShape。
     * @param light          光照值。
     * @param overlay        覆蓋層。
     */
    public static void renderShape(PoseStack poseStack, VertexConsumer vertexConsumer, VoxelShape shape, int light, int overlay) {
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            renderBox(poseStack, vertexConsumer, minX, minY, minZ, maxX, maxY, maxZ, light, overlay);
        });
    }

    /**
     * 渲染一個方塊 AABB。
     *
     * @param poseStack      當前的 PoseStack。
     * @param vertexConsumer 目標 VertexConsumer。
     * @param minX           AABB 的最小 X。
     * @param minY           AABB 的最小 Y。
     * @param minZ           AABB 的最小 Z。
     * @param maxX           AABB 的最大 X。
     * @param maxY           AABB 的最大 Y。
     * @param maxZ           AABB 的最大 Z。
     * @param light          光照值。
     * @param overlay        覆蓋層。
     */
    public static void renderBox(PoseStack poseStack, VertexConsumer vertexConsumer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int light, int overlay) {
        // 北面
        addVertex(poseStack, vertexConsumer, minX, minY, minZ, light, overlay, 0, 0, 0, 0, -1);
        addVertex(poseStack, vertexConsumer, maxX, minY, minZ, light, overlay, 1, 0, 0, 0, -1);
        addVertex(poseStack, vertexConsumer, maxX, maxY, minZ, light, overlay, 1, 1, 0, 0, -1);
        addVertex(poseStack, vertexConsumer, minX, maxY, minZ, light, overlay, 0, 1, 0, 0, -1);

        // 南面
        addVertex(poseStack, vertexConsumer, minX, minY, maxZ, light, overlay, 0, 0, 0, 0, 1);
        addVertex(poseStack, vertexConsumer, maxX, minY, maxZ, light, overlay, 1, 0, 0, 0, 1);
        addVertex(poseStack, vertexConsumer, maxX, maxY, maxZ, light, overlay, 1, 1, 0, 0, 1);
        addVertex(poseStack, vertexConsumer, minX, maxY, maxZ, light, overlay, 0, 1, 0, 0, 1);

        // 類似地處理東面、西面、上面和下面
    }


    private static void addVertex(PoseStack poseStack, VertexConsumer vertexConsumer, double x, double y, double z, int light, int overlay, float u, float v, float normalX, float normalY, float normalZ) {
        vertexConsumer.vertex(poseStack.last().pose(), (float) x, (float) y, (float) z)
                .color(255, 255, 255, 255) // 白色
                .uv(u, v) // 動態 UV
                .overlayCoords(overlay)
                .uv2(light)
                .normal(poseStack.last().normal(), normalX, normalY, normalZ) // 動態法線
                .endVertex();
    }

    /*public static void renderManaFlow(PoseStack poseStack, MultiBufferSource bufferSource, ManaConduitBlockEntity blockEntity, float partialTicks, int packedLight) {
        // 設定魔力流動動畫的參數
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.glowingEffect());
        // 自定義動畫邏輯
        float progress = blockEntity.getManaFlowProgress(partialTicks);
        // 渲染流動效果
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5); // 中心點
        // 使用頂點渲染動畫
        vertexConsumer.vertex(0, 0, 0).color(0, 255, 255, 128).endVertex();
        // 渲染邏輯繼續...
        poseStack.popPose();
    }*/

}
