package com.lazychara.skijatest.client.Musicpage;

final class MusicPageMath {


    static float easeOutCubic(float t) {
        t = clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    static float easeInOut(float t) {
        t = clamp(t, 0f, 1f);
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2f) * 0.5f;
    }

    static float easeInOutBack(float t) {
        t = clamp(t, 0f, 1f);
        float c2 = 1.70158f * 1.525f;
        if (t < 0.5f) return (float) Math.pow(2f * t, 2f) * ((c2 + 1f) * 2f * t - c2) * 0.5f;
        return ((float) Math.pow(2f * t - 2f, 2f) * ((c2 + 1f) * (t * 2f - 2f) + c2) + 2f) * 0.5f;
    }

    static float easeOutExpo(float t) {
        t = clamp(t, 0f, 1f);
        return t == 1f ? 1f : 1f - (float) Math.pow(2f, -10f * t);
    }

    static float clamp01(float v) {
        return clamp(v, 0f, 1f);
    }

    static float clampPositive(float v) {
        return Math.max(0f, v);
    }


    static float sin(float v) { return (float) Math.sin(v); }
    static float cos(float v) { return (float) Math.cos(v); }
    static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    static float alphaOf(int color) { return ((color >>> 24) & 0xFF) / 255f; }

    static int rgb(int r, int g, int b) {
        return 0xFF000000 | (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b);
    }

    static int clamp8(int v) { return Math.max(0, Math.min(255, v)); }

    static int lighten(int color, float amount) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return rgb((int)(r + (255 - r) * amount), (int)(g + (255 - g) * amount), (int)(b + (255 - b) * amount));
    }

    static int darken(int color, float amount) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return rgb((int)(r * (1f - amount)), (int)(g * (1f - amount)), (int)(b * (1f - amount)));
    }

    static int rotate(int color) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return rgb(Math.max(r, b), Math.max(g - 8, 0), Math.max(r - 20, 0));
    }

    static int saturate(int color, float factor) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        float gray = (r + g + b) / 3f;
        return rgb((int)(gray + (r - gray) * factor), (int)(gray + (g - gray) * factor), (int)(gray + (b - gray) * factor));
    }



    private MusicPageMath() {}
}
