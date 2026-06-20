package com.lazychara.skijatest.client.Musicpage;

import com.lazychara.skijatest.client.SkijaRenderer;
import io.github.humbleui.skija.Image;

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


