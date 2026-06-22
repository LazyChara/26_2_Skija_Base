package com.lazychara.skijatest.client.Musicpage;

import java.util.List;
import com.lazychara.skijatest.client.SkijaRenderer;
import io.github.humbleui.skija.Image;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;

record EmphasisSlice(float startX, float endX) {}

record HorizontalGrid(float coverY, float controlsY, float controlsH, float lyricTop, float lyricH) {}

record InterludeState(boolean active, int anchor, float start, float end, boolean nextDuet) {}

record LyricLine(float startTime, float endTime, String text, boolean isBG,
                 List<LyricWord> words, String translation, String romanization, boolean isDuet) {
    public LyricLine(float startTime, float endTime, String text, boolean isBG,
                     List<LyricWord> words, String translation, boolean isDuet) {
        this(startTime, endTime, text, isBG, words, translation, null, isDuet);
    }
    public LyricLine(float startTime, float endTime, String text, boolean isBG) {
        this(startTime, endTime, text, isBG, null, null, null, false);
    }
    public boolean isDynamic() { return words != null && !words.isEmpty(); }
}

record LyricPresentation(float opacity, float scale) {}

record LyricWord(float startTime, float endTime, String word, String romanWord, List<RubyText> ruby) {
    public LyricWord(float startTime, float endTime, String word) {
        this(startTime, endTime, word, null, List.of());
    }
    public LyricWord(float startTime, float endTime, String word, String romanWord) {
        this(startTime, endTime, word, romanWord, List.of());
    }
    public LyricWord withRomanWord(String romanWord) {
        return new LyricWord(startTime, endTime, word, romanWord, ruby);
    }
}

record LyricWordPlacement(int lineIndex, float startX, float endX, String text) {}

record RomanWordBox(float startX, float endX, float y, float h) {}

record RubyText(float startTime, float endTime, String text) {}

record SpringParams(float stiffness, float damping) {}

record TextSegment(String text, float width, boolean isSpace) {}

record WordRange(float startTime, float endTime,
                 float startX, float endX,
                 float y, float h,
                 float baseY, float baseH,
                 float rubyY, float rubyH,
                 float romanY, float romanH, float romanStartX, float romanEndX,
                 float romanWordStartX, float romanWordEndX, float romanWordY, float romanWordH,
                 String wordText, List<EmphasisSlice> emphasisSlices,
                 int lineIndex, int wordIndex, int wordCount,
                 boolean emphasize, List<RubyText> ruby) {}

final class TextImage {
    final Image image;
    final SkijaRenderer renderer;
    final float w;
    final float h;
    final float pad;

    TextImage(Image image, SkijaRenderer renderer, float w, float h, float pad) {
        this.image = image;
        this.renderer = renderer;
        this.w = w;
        this.h = h;
        this.pad = pad;
    }
}

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
