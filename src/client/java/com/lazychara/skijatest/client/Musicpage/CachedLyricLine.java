package com.lazychara.skijatest.client.Musicpage;

import java.util.List;
import com.lazychara.skijatest.client.SkijaRenderer;
import io.github.humbleui.skija.Image;

final class CachedLyricLine implements AutoCloseable {
    LyricLine lineRef;
    Image whiteImage;
    SkijaRenderer whiteRenderer;
    final Image activeImage;
    final SkijaRenderer activeRenderer;
    final float activeW;
    final float activeH;
    final float activePad;
    final Image inactiveImage;
    final SkijaRenderer inactiveRenderer;
    final float inactiveW;
    final float inactiveH;
    final float inactivePad;
    final Image hoverImage;
    final SkijaRenderer hoverRenderer;
    final float hoverW;
    final float hoverH;
    final float hoverPad;
    List<WordRange> dynamicWordRanges = List.of();
    final AMLLSpring ySpring = new AMLLSpring(0f);
    final AMLLSpring scaleSpring = new AMLLSpring(1f);
    float lastDelayedTargetY = Float.NaN;
    float currentY;
    float velocityY;
    float currentScale = 1f;
    float velocityScale;
    float currentOpacity = 1f;
    float velocityOpacity;
    float currentBlurVisual;
    float velocityBlur;
    float currentBrightAlpha = 1f;
    float currentDarkAlpha = 0.2f;
    float targetBrightAlpha = 1f;
    float targetDarkAlpha = 0.2f;
    boolean initialized;

    CachedLyricLine(TextImage active, TextImage inactive, TextImage hover) {
        this.activeImage = active.image;
        this.activeRenderer = active.renderer;
        this.activeW = active.w;
        this.activeH = active.h;
        this.activePad = active.pad;
        this.inactiveImage = inactive.image;
        this.inactiveRenderer = inactive.renderer;
        this.inactiveW = inactive.w;
        this.inactiveH = inactive.h;
        this.inactivePad = inactive.pad;
        this.hoverImage = hover.image;
        this.hoverRenderer = hover.renderer;
        this.hoverW = hover.w;
        this.hoverH = hover.h;
        this.hoverPad = hover.pad;
    }

    void setDynamicWordRanges(List<WordRange> ranges) {
        this.dynamicWordRanges = ranges == null ? List.of() : ranges;
    }

    @Override
    public void close() {
        if (whiteImage != null) whiteImage.close();
        if (whiteRenderer != null) whiteRenderer.close();
        if (activeImage != null) activeImage.close();
        if (inactiveImage != null) inactiveImage.close();
        if (hoverImage != null) hoverImage.close();
        if (activeRenderer != null) activeRenderer.close();
        if (inactiveRenderer != null) inactiveRenderer.close();
        if (hoverRenderer != null) hoverRenderer.close();
    }
}
