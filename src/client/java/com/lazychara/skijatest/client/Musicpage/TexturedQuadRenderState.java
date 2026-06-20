package com.lazychara.skijatest.client.Musicpage;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;

record TexturedQuadRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
                                       float x, float y, float w, float h,
                                       float u0, float v0, float u1, float v1,
                                       int color, ScreenRectangle scissorArea, ScreenRectangle bounds)
        implements net.minecraft.client.renderer.state.gui.GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x, y).setUv(u0, v0).setColor(color);
        vertexConsumer.addVertexWith2DPose(pose, x, y + h).setUv(u0, v1).setColor(color);
        vertexConsumer.addVertexWith2DPose(pose, x + w, y + h).setUv(u1, v1).setColor(color);
        vertexConsumer.addVertexWith2DPose(pose, x + w, y).setUv(u1, v0).setColor(color);
    }
}


