package com.lazychara.skijatest.client;

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;

import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;

import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Paint;

import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Shader;

import io.github.humbleui.types.Point;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;


import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.Random;

import java.util.concurrent.atomic.AtomicLong;


public final class AMLLFluidBackground implements AutoCloseable {
    private static final int TEXTURE_SIZE = 32;
    private static final int SUBDIVISIONS = 50;
    private static final PointConf[][][] OFFICIAL_PRESETS = new PointConf[][][]{
            {
                    {pc(-1f, -1f, 0f, 0f, 1f, 1f), pc(-0.33333334f, -1f, 0f, 0f, 1f, 1f), pc(0.33333334f, -1f, 0f, 0f, 1f, 1f), pc(1f, -1f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, -0.044954f, 0f, 0f, 1f, 1f), pc(-0.24056117f, -0.22465999f, 0f, 0f, 1f, 1f), pc(0.33475888f, -0.005312972f, 0f, 0f, 1f, 1f), pc(0.998992f, -0.3382976f, 8f, 0f, 0.566f, 1.792f)},
                    {pc(-1f, 0.33333334f, 0f, 0f, 1f, 1f), pc(-0.34254974f, 0f, 0f, 0f, 1f, 1f), pc(0.33214378f, 0.19817764f, 0f, 0f, 1f, 1f), pc(1f, 0.07661182f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, 1f, 0f, 0f, 1f, 1f), pc(-0.33333334f, 1f, 0f, 0f, 1f, 1f), pc(0.33333334f, 1f, 0f, 0f, 1f, 1f), pc(1f, 1f, 0f, 0f, 1f, 1f)}
            },
            {
                    {pc(-1f, -1f, 0f, 0f, 1f, 2.075f), pc(-0.33333334f, -1f, 0f, 0f, 1f, 1f), pc(0.33333334f, -1f, 0f, 0f, 1f, 1f), pc(1f, -1f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, -0.45457795f, 0f, 0f, 1f, 1f), pc(-0.33333334f, -0.33333334f, 0f, 0f, 1f, 1f), pc(0.088940315f, -0.6025711f, -32f, 45f, 1f, 1f), pc(1f, -0.33333334f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, -0.07402409f, 1f, 0f, 1f, 0.094f), pc(-0.27194226f, 0.0977537f, 25f, -18f, 1.321f, 0f), pc(0.19877414f, 0.43073833f, 48f, -40f, 0.755f, 0.975f), pc(1f, 0.33333334f, -37f, 0f, 1f, 1f)},
                    {pc(-1f, 1f, 0f, 0f, 1f, 1f), pc(-0.33333334f, 1f, 0f, 0f, 1f, 1f), pc(0.5125851f, 1f, -20f, -18f, 0f, 1.604f), pc(1f, 1f, 0f, 0f, 1f, 1f)}
            },
            {
                    {pc(-1f, -1f, 0f, 0f, 1f, 1f), pc(-0.5f, -1f, 0f, 0f, 1f, 1f), pc(0f, -1f, 0f, 0f, 1f, 1f), pc(0.5f, -1f, 0f, 0f, 1f, 1f), pc(1f, -1f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, -0.5f, 0f, 0f, 1f, 1f), pc(-0.5f, -0.5f, 0f, 0f, 1f, 1f), pc(-0.0052029684f, -0.6131421f, 0f, 0f, 1f, 1f), pc(0.5884227f, -0.3990805f, 0f, 0f, 1f, 1f), pc(1f, -0.5f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, 0f, 0f, 0f, 1f, 1f), pc(-0.42100248f, -0.11895058f, 0f, 0f, 1f, 1f), pc(-0.10196134f, -0.023812119f, 0f, -47f, 0.629f, 0.849f), pc(0.40275127f, -0.063453145f, 0f, 0f, 1f, 1f), pc(1f, 0f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, 0.5f, 0f, 0f, 1f, 1f), pc(0.068019584f, 0.5205913f, -31f, -45f, 1f, 1f), pc(0.2144647f, 0.2933161f, 6f, -56f, 0.566f, 1.321f), pc(0.5f, 0.5f, 0f, 0f, 1f, 1f), pc(1f, 0.5f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, 1f, 0f, 0f, 1f, 1f), pc(-0.31378374f, 1f, 0f, 0f, 1f, 1f), pc(0.26153633f, 1f, 0f, 0f, 1f, 1f), pc(0.5f, 1f, 0f, 0f, 1f, 1f), pc(1f, 1f, 0f, 0f, 1f, 1f)}
            },
            {
                    {pc(-1f, -1f, 0f, 0f, 1f, 1f), pc(-0.4501953f, -1f, 0f, 55f, 1f, 2.075f), pc(0.1953125f, -1f, 0f, 0f, 1f, 1f), pc(0.4580078f, -1f, 0f, -25f, 1f, 1f), pc(1f, -1f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, -0.25144753f, -16f, 0f, 2.327f, 0.943f), pc(-0.55859375f, -0.6609326f, 47f, 0f, 2.358f, 0.377f), pc(0.23242188f, -0.52443755f, -66f, -25f, 1.855f, 1.164f), pc(0.6855469f, -0.37537065f, 0f, 0f, 1f, 1f), pc(1f, -0.6699125f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, 0.035910398f, 0f, 0f, 1f, 1f), pc(-0.4921875f, 0.005378616f, 90f, 23f, 1f, 1.981f), pc(0.021484375f, -0.13650437f, 0f, 42f, 1f, 1f), pc(0.4765625f, 0.05925823f, -30f, 0f, 1.95f, 0.44f), pc(1f, 0.25142884f, 0f, 0f, 1f, 1f)},
                    {pc(-1f, 0.69683367f, -68f, 0f, 1f, 0.786f), pc(-0.6904297f, 0.58907443f, -68f, 0f, 1f, 1f), pc(0.18457031f, 0.38792387f, 61f, 0f, 1f, 1f), pc(0.60546875f, 0.46335533f, -47f, -59f, 0.849f, 1.73f), pc(1f, 0.6214022f, -33f, 0f, 0.377f, 1.604f)},
                    {pc(-1f, 1f, 0f, 0f, 1f, 1f), pc(-0.5f, 1f, 0f, -73f, 1f, 1f), pc(-0.32714844f, 1f, 0f, -24f, 0.314f, 2.704f), pc(0.5f, 1f, 0f, 0f, 1f, 1f), pc(1f, 1f, 0f, 0f, 1f, 1f)}
            },
            {
                    {pc(-1f, -1f), pc(-0.6393f, -1f, 0f, 0f, 1f, 2.3884f), pc(0f, -1f), pc(0.5f, -1f), pc(1f, -1f)},
                    {pc(-1f, -0.2301f), pc(-0.6934f, -0.331f, 0f, -0.7188f, 1f, 1.063f), pc(-0.0082f, -0.6814f, -0.2583f, 0f, 1.0964f, 1f), pc(0.5836f, -0.531f, 0.7029f, 0f, 1.5466f, 1f), pc(1f, -0.6407f)},
                    {pc(-1f, 0.2973f, 0f, 0f, 1.8352f, 1f), pc(-0.4082f, 0.0602f), pc(-0.1803f, -0.3646f, -0.2998f, 0f, 1.1513f, 1f), pc(0.477f, -0.1027f, 0.8903f, -0.1882f, 1.0807f, 0.8551f), pc(1f, -0.2973f)},
                    {pc(-1f, 0.7628f, 0f, 0f, 2.3868f, 1f), pc(-0.2525f, 0.4814f, -0.8406f, -1.6199f, 1.4093f, 1.2215f), pc(0.3607f, 0.2814f, -1.0713f, -0.0529f, 1.0025f, 0.7611f), pc(0.4885f, 0.623f, 0f, 0.8184f, 1f, 1.2876f), pc(1f, 0.5f)},
                    {pc(-1f, 1f), pc(-0.4033f, 1f), pc(0.2672f, 1f), pc(0.5967f, 1f), pc(1f, 1f)}
            },
            {
                    {pc(-1f, -1f), pc(-0.2197f, -1f), pc(0.0197f, -1f), pc(0.8033f, -1f), pc(1f, -1f)},
                    {pc(-1f, -0.5451f), pc(-0.4885f, -0.4035f, -1.0246f, -0.2268f, 1.1936f, 0.8005f), pc(-0.1213f, -0.2867f, 0f, -0.6981f, 1f, 0.809f), pc(0.3246f, -0.5628f, 0f, -1.2188f, 1f, 1.044f), pc(1f, -0.3292f)},
                    {pc(-1f, 0.1416f), pc(-0.341f, -0.0142f, 0f, -0.4004f, 1f, 1.1293f), pc(-0.0393f, -0.023f, 0.2915f, -0.373f, 1.044f, 0.9879f), pc(0.3148f, -0.0673f, -0.7853f, -0.8962f, 1.4709f, 1.0247f), pc(1f, 0.1912f)},
                    {pc(-1f, 0.5f), pc(-0.2689f, 0.2743f, 0.3404f, -0.5248f, 1.0184f, 0.4391f), pc(0.0721f, 0.269f, 0.5302f, 0.1244f, 0.6723f, 0.3225f), pc(0.4148f, 0.3894f, -0.6977f, -0.6783f, 0.8094f, 0.9247f), pc(1f, 0.446f)},
                    {pc(-1f, 1f), pc(-0.7311f, 1f), pc(0.323f, 1f), pc(0.6393f, 1f), pc(1f, 1f)}
            }
    };

    private Image textureImage;
    private Shader textureShader;
    private Paint meshPaint;
    private int[] texturePixelsABGR;
    private ControlPoint[][] controlPoints;
    private int controlW;
    private int controlH;
    private int gridW;
    private int gridH;
    private int vertexCount;
    private short[] indices;
    private int[] colors;
    private float[] baseUvs;
    private Point[] positions;
    private Point[] texCoords;
    private float lastWidth = -1f;
    private float lastHeight = -1f;
    private long seed;

    private static PointConf pc(float x, float y) {
        return new PointConf(x, y, 0f, 0f, 1f, 1f);
    }

    private static PointConf pc(float x, float y, float ur, float vr, float up, float vp) {
        return new PointConf(x, y, ur, vr, up, vp);
    }

    public Image image() {
        return textureImage;
    }

    public void rebuild(byte[] coverBytes, String seedText) {
        rebuild(coverBytes, seedText, 0, 0);
    }

    @SuppressWarnings("deprecation")
    public void rebuild(byte[] coverBytes, String seedText, int width, int height) {
        closeImage();
        try {
            seed = seedText == null ? 0L : seedText.hashCode();
            BufferedImage cover = null;
            if (coverBytes != null && coverBytes.length > 0) {
                cover = ImageIO.read(new ByteArrayInputStream(coverBytes));
            }
            BufferedImage texture = cover != null ? makeAlbumTexture(cover) : makeFallbackTexture(seed);
            texturePixelsABGR = extractTexturePixelsABGR(texture);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(texture, "png", out);
                textureImage = Image.makeFromEncoded(out.toByteArray());
            }
            rebuildShader();
            controlPoints = generateControlPoints(seed);
            buildStaticMeshData();
            positions = null;
            texCoords = null;
            lastWidth = -1f;
            lastHeight = -1f;
        } catch (Exception e) {
            SkijaTestClient.LOGGER.warn("[AMLLFluidBackground] Failed to rebuild mesh background", e);
            closeImage();
            texturePixelsABGR = null;
            controlPoints = null;
            controlW = 0;
            controlH = 0;
            gridW = 0;
            gridH = 0;
            vertexCount = 0;
            positions = null;
            texCoords = null;
        }
    }

    public boolean draw(Canvas canvas, float width, float height, float time, float lowFreqVolume) {
        if (textureImage == null || textureShader == null || meshPaint == null || controlPoints == null) return false;
        ensurePositions(width, height);
        updateTextureCoordinates(time, lowFreqVolume);
        canvas.drawTriangles(positions, colors, texCoords, indices, BlendMode.MODULATE, meshPaint);
        return true;
    }

    public boolean fillGpuPositions(float width, float height, float[] out) {
        if (controlPoints == null || out == null || out.length < vertexCount * 2) return false;
        float aspect = Math.max(0.01f, width / Math.max(1f, height));
        float subdivM1 = SUBDIVISIONS - 1f;
        for (int gy = 0; gy < gridH; gy++) {
            int cellY = Math.min(controlH - 2, gy / SUBDIVISIONS);
            float v = (gy - cellY * SUBDIVISIONS) / subdivM1;
            for (int gx = 0; gx < gridW; gx++) {
                int cellX = Math.min(controlW - 2, gx / SUBDIVISIONS);
                float u = (gx - cellX * SUBDIVISIONS) / subdivM1;
                float[] p = evalPatch(cellX, cellY, u, v);
                float px = p[0];
                float py = p[1];
                if (aspect > 1f) py *= aspect;
                else px /= aspect;
                int i = (gy * gridW + gx) * 2;
                out[i] = (px + 1f) * 0.5f * width;
                out[i + 1] = (py + 1f) * 0.5f * height;
            }
        }
        return true;
    }

    public boolean fillGpuTexCoords(float time, float lowFreqVolume, float[] out) {
        if (baseUvs == null || out == null || out.length < vertexCount * 2) return false;
        float volume = clamp(lowFreqVolume, 0f, 0.45f);
        float uTime = time * 0.24f;
        float angle = (uTime + volume) * 2.0f;
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float scale = Math.max(0.62f, 1.0f - volume * 2.0f);
        for (int i = 0; i < vertexCount; i++) {
            float u = baseUvs[i * 2];
            float v = baseUvs[i * 2 + 1];
            float cx = u - 0.2f;
            float cy = v - 0.2f;
            float ru = cos * cx - sin * cy;
            float rv = sin * cx + cos * cy;
            out[i * 2] = ru * scale + 0.5f;
            out[i * 2 + 1] = rv * scale + 0.5f;
        }
        return true;
    }

    public int[] copyTexturePixelsABGR() {
        return texturePixelsABGR == null ? null : texturePixelsABGR.clone();
    }

    public int gridWidth() {
        return gridW;
    }

    public int gridHeight() {
        return gridH;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public static int textureSize() {
        return TEXTURE_SIZE;
    }

    private int[] extractTexturePixelsABGR(BufferedImage texture) {
        int[] out = new int[TEXTURE_SIZE * TEXTURE_SIZE];
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int argb = texture.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                out[y * TEXTURE_SIZE + x] = (a << 24) | (b << 16) | (g << 8) | r;
            }
        }
        return out;
    }

    private BufferedImage makeAlbumTexture(BufferedImage cover) {
        BufferedImage texture = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = texture.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(cover, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, null);
        } finally {
            g.dispose();
        }
        processLikeAMLL(texture);
        boxBlur(texture, 2, 4);
        addDither(texture, seed);
        return texture;
    }

    private BufferedImage makeFallbackTexture(long seed) {
        BufferedImage texture = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(seed ^ 0xA771BEEFL);
        int a = fallbackColor(random, 100, 58, 96);
        int b = fallbackColor(random, 76, 70, 128);
        int c = fallbackColor(random, 130, 48, 72);
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            float v = y / (float) (TEXTURE_SIZE - 1);
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                float u = x / (float) (TEXTURE_SIZE - 1);
                float n = smoothNoise(u * 3.2f + seed * 0.0013f, v * 3.2f - seed * 0.0017f);
                int ab = mix(a, b, smoothstep(0.0f, 1.0f, u * 0.85f + n * 0.25f));
                int bc = mix(b, c, smoothstep(0.0f, 1.0f, v * 0.75f + (1f - n) * 0.30f));
                texture.setRGB(x, y, mix(ab, bc, 0.45f + (n - 0.5f) * 0.24f));
            }
        }
        processLikeAMLL(texture);
        boxBlur(texture, 2, 4);
        addDither(texture, seed);
        return texture;
    }

    private int fallbackColor(Random random, int rBase, int gBase, int bBase) {
        int r = clamp8(rBase + random.nextInt(92));
        int g = clamp8(gBase + random.nextInt(82));
        int b = clamp8(bBase + random.nextInt(94));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void processLikeAMLL(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                float r = (argb >>> 16) & 0xFF;
                float g = (argb >>> 8) & 0xFF;
                float b = argb & 0xFF;

                r = (r - 128f) * 0.4f + 128f;
                g = (g - 128f) * 0.4f + 128f;
                b = (b - 128f) * 0.4f + 128f;

                float gray = r * 0.30f + g * 0.59f + b * 0.11f;
                r = gray * -2.0f + r * 3.0f;
                g = gray * -2.0f + g * 3.0f;
                b = gray * -2.0f + b * 3.0f;

                r = (r - 128f) * 1.7f + 128f;
                g = (g - 128f) * 1.7f + 128f;
                b = (b - 128f) * 1.7f + 128f;

                r *= 0.75f;
                g *= 0.75f;
                b *= 0.75f;

                img.setRGB(x, y, 0xFF000000 | (clamp8(Math.round(r)) << 16) | (clamp8(Math.round(g)) << 8) | clamp8(Math.round(b)));
            }
        }
    }

    private ControlPoint[][] generateControlPoints(long seed) {
        Random random = new Random(seed * 31L + 0x51A7C0DEL);
        PointConf[][] conf = random.nextFloat() > 0.8f
                ? generateRandomControlPointConf(random, 6, 6)
                : copyPreset(OFFICIAL_PRESETS[Math.floorMod((int) (seed ^ (seed >>> 32)), OFFICIAL_PRESETS.length)]);
        return buildControlPoints(conf);
    }

    private PointConf[][] copyPreset(PointConf[][] preset) {
        int h = preset.length;
        int w = preset[0].length;
        PointConf[][] out = new PointConf[h][w];
        for (int y = 0; y < h; y++) {
            System.arraycopy(preset[y], 0, out[y], 0, w);
        }
        return out;
    }

    private PointConf[][] generateRandomControlPointConf(Random random, int w, int h) {
        PointConf[][] conf = new PointConf[h][w];
        float dx = 2f / (w - 1);
        float dy = 2f / (h - 1);
        float variationFraction = randomRange(random, 0.40f, 0.60f);
        float normalOffset = randomRange(random, 0.30f, 0.60f);
        float smoothFactor = randomRange(random, 0.20f, 0.30f);
        float smoothModifier = randomRange(random, -0.10f, -0.05f);
        int smoothIters = 3 + random.nextInt(2);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float baseX = (x / (float) (w - 1)) * 2f - 1f;
                float baseY = (y / (float) (h - 1)) * 2f - 1f;
                boolean border = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                float px = baseX;
                float py = baseY;
                if (!border) {
                    px += randomRange(random, -variationFraction * dx, variationFraction * dx);
                    py += randomRange(random, -variationFraction * dy, variationFraction * dy);
                    float uNorm = (baseX + 1f) * 0.5f;
                    float vNorm = (baseY + 1f) * 0.5f;
                    float[] gradient = computeNoiseGradient(uNorm, vNorm);
                    float distToBorder = Math.min(Math.min(uNorm, 1f - uNorm), Math.min(vNorm, 1f - vNorm));
                    float weight = smoothstep(0f, 1f, distToBorder);
                    px += gradient[0] * normalOffset * weight;
                    py += gradient[1] * normalOffset * weight;
                }
                float ur = border ? 0f : randomRange(random, -60f, 60f);
                float vr = border ? 0f : randomRange(random, -60f, 60f);
                float up = border ? 1f : randomRange(random, 0.80f, 1.20f);
                float vp = border ? 1f : randomRange(random, 0.80f, 1.20f);
                conf[y][x] = new PointConf(px, py, ur, vr, up, vp);
            }
        }
        smoothifyControlPoints(conf, smoothIters, smoothFactor, smoothModifier);
        return conf;
    }

    private ControlPoint[][] buildControlPoints(PointConf[][] conf) {
        controlH = conf.length;
        controlW = conf[0].length;
        gridW = (controlW - 1) * SUBDIVISIONS;
        gridH = (controlH - 1) * SUBDIVISIONS;
        vertexCount = gridW * gridH;
        ControlPoint[][] out = new ControlPoint[controlH][controlW];
        float uPower = 2f / (controlW - 1);
        float vPower = 2f / (controlH - 1);
        for (int y = 0; y < controlH; y++) {
            for (int x = 0; x < controlW; x++) {
                PointConf p = conf[y][x];
                float uRot = (float) Math.toRadians(p.ur);
                float vRot = (float) Math.toRadians(p.vr);
                float uScale = uPower * p.up;
                float vScale = vPower * p.vp;
                out[y][x] = new ControlPoint(
                        p.x,
                        p.y,
                        (float) Math.cos(uRot) * uScale,
                        (float) Math.sin(uRot) * uScale,
                        (float) -Math.sin(vRot) * vScale,
                        (float) Math.cos(vRot) * vScale);
            }
        }
        return out;
    }

    private void smoothifyControlPoints(PointConf[][] grid, int iterations, float factor, float modifier) {
        float f = factor;
        int h = grid.length;
        int w = grid[0].length;
        float[][] kernel = {
                {1f, 2f, 1f},
                {2f, 4f, 2f},
                {1f, 2f, 1f}
        };
        for (int iter = 0; iter < iterations; iter++) {
            PointConf[][] next = new PointConf[h][w];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (x == 0 || x == w - 1 || y == 0 || y == h - 1) {
                        next[y][x] = grid[y][x];
                        continue;
                    }
                    float sx = 0, sy = 0, sur = 0, svr = 0, sup = 0, svp = 0;
                    for (int oy = -1; oy <= 1; oy++) {
                        for (int ox = -1; ox <= 1; ox++) {
                            float k = kernel[oy + 1][ox + 1];
                            PointConf p = grid[y + oy][x + ox];
                            sx += p.x * k;
                            sy += p.y * k;
                            sur += p.ur * k;
                            svr += p.vr * k;
                            sup += p.up * k;
                            svp += p.vp * k;
                        }
                    }
                    PointConf cur = grid[y][x];
                    next[y][x] = new PointConf(
                            lerp(cur.x, sx / 16f, f),
                            lerp(cur.y, sy / 16f, f),
                            lerp(cur.ur, sur / 16f, f),
                            lerp(cur.vr, svr / 16f, f),
                            lerp(cur.up, sup / 16f, f),
                            lerp(cur.vp, svp / 16f, f));
                }
            }
            for (int y = 0; y < h; y++) {
                System.arraycopy(next[y], 0, grid[y], 0, w);
            }
            f = clamp01(f + modifier);
        }
    }

    private void buildStaticMeshData() {
        indices = new short[(gridW - 1) * (gridH - 1) * 6];
        int idx = 0;
        for (int y = 0; y < gridH - 1; y++) {
            for (int x = 0; x < gridW - 1; x++) {
                int p00 = y * gridW + x;
                int p10 = y * gridW + x + 1;
                int p01 = (y + 1) * gridW + x;
                int p11 = (y + 1) * gridW + x + 1;
                indices[idx++] = (short) p00;
                indices[idx++] = (short) p10;
                indices[idx++] = (short) p01;
                indices[idx++] = (short) p10;
                indices[idx++] = (short) p11;
                indices[idx++] = (short) p01;
            }
        }
        colors = new int[vertexCount];
        baseUvs = new float[vertexCount * 2];
        float subdivM1 = SUBDIVISIONS - 1f;
        float invTH = 1f / (subdivM1 * (controlW - 1));
        float invTW = 1f / (subdivM1 * (controlH - 1));
        for (int y = 0; y < gridH; y++) {
            int cellY = Math.min(controlH - 2, y / SUBDIVISIONS);
            int localY = y - cellY * SUBDIVISIONS;
            float sY = cellY / (float) (controlH - 1);
            for (int x = 0; x < gridW; x++) {
                int cellX = Math.min(controlW - 2, x / SUBDIVISIONS);
                int localX = x - cellX * SUBDIVISIONS;
                float sX = cellX / (float) (controlW - 1);
                int i = y * gridW + x;
                colors[i] = 0xFFFFFFFF;
                baseUvs[i * 2] = sX + localX * invTH;
                baseUvs[i * 2 + 1] = 1f - sY - localY * invTW;
            }
        }
    }

    private void ensurePositions(float width, float height) {
        if (positions != null && Math.abs(lastWidth - width) < 0.5f && Math.abs(lastHeight - height) < 0.5f) return;
        positions = new Point[vertexCount];
        float aspect = Math.max(0.01f, width / Math.max(1f, height));
        float subdivM1 = SUBDIVISIONS - 1f;
        for (int gy = 0; gy < gridH; gy++) {
            int cellY = Math.min(controlH - 2, gy / SUBDIVISIONS);
            float v = (gy - cellY * SUBDIVISIONS) / subdivM1;
            for (int gx = 0; gx < gridW; gx++) {
                int cellX = Math.min(controlW - 2, gx / SUBDIVISIONS);
                float u = (gx - cellX * SUBDIVISIONS) / subdivM1;
                float[] p = evalPatch(cellX, cellY, u, v);
                float px = p[0];
                float py = p[1];
                if (aspect > 1f) py *= aspect;
                else px /= aspect;
                positions[gy * gridW + gx] = new Point((px + 1f) * 0.5f * width, (py + 1f) * 0.5f * height);
            }
        }
        lastWidth = width;
        lastHeight = height;
    }

    private void updateTextureCoordinates(float time, float lowFreqVolume) {
        if (texCoords == null || texCoords.length != vertexCount) {
            texCoords = new Point[vertexCount];
        }
        float volume = clamp(lowFreqVolume, 0f, 0.45f);
        float uTime = time * 0.24f;
        float angle = (uTime + volume) * 2.0f;
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float scale = Math.max(0.62f, 1.0f - volume * 2.0f);
        float texMax = TEXTURE_SIZE - 1f;
        for (int i = 0; i < vertexCount; i++) {
            float u = baseUvs[i * 2];
            float v = baseUvs[i * 2 + 1];
            float cx = u - 0.2f;
            float cy = v - 0.2f;
            float ru = cos * cx - sin * cy;
            float rv = sin * cx + cos * cy;
            float finalU = ru * scale + 0.5f;
            float finalV = rv * scale + 0.5f;
            texCoords[i] = new Point(finalU * texMax, finalV * texMax);
        }
    }

    private float[] evalPatch(int x, int y, float u, float v) {
        ControlPoint p00 = controlPoints[y][x];
        ControlPoint p10 = controlPoints[y][x + 1];
        ControlPoint p01 = controlPoints[y + 1][x];
        ControlPoint p11 = controlPoints[y + 1][x + 1];

        float ax = hermite(p00.x, p10.x, p00.ux, p10.ux, u);
        float ay = hermite(p00.y, p10.y, p00.uy, p10.uy, u);
        float bx = hermite(p01.x, p11.x, p01.ux, p11.ux, u);
        float by = hermite(p01.y, p11.y, p01.uy, p11.uy, u);

        float avx = hermite(p00.vx, p10.vx, 0f, 0f, u);
        float avy = hermite(p00.vy, p10.vy, 0f, 0f, u);
        float bvx = hermite(p01.vx, p11.vx, 0f, 0f, u);
        float bvy = hermite(p01.vy, p11.vy, 0f, 0f, u);

        return new float[]{
                hermite(ax, bx, avx, bvx, v),
                hermite(ay, by, avy, bvy, v)
        };
    }

    private float hermite(float p0, float p1, float m0, float m1, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return (2f * t3 - 3f * t2 + 1f) * p0
                + (t3 - 2f * t2 + t) * m0
                + (-2f * t3 + 3f * t2) * p1
                + (t3 - t2) * m1;
    }

    private void boxBlur(BufferedImage img, int radius, int quality) {
        if (radius <= 0) return;
        int w = img.getWidth();
        int h = img.getHeight();
        int[] src = new int[w * h];
        int[] dst = new int[w * h];
        img.getRGB(0, 0, w, h, src, 0, w);
        while (quality-- > 0) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int tr = 0, tg = 0, tb = 0, count = 0;
                    for (int xx = Math.max(0, x - radius); xx <= Math.min(w - 1, x + radius); xx++) {
                        int c = src[y * w + xx];
                        tr += (c >>> 16) & 0xFF;
                        tg += (c >>> 8) & 0xFF;
                        tb += c & 0xFF;
                        count++;
                    }
                    dst[y * w + x] = 0xFF000000 | ((tr / count) << 16) | ((tg / count) << 8) | (tb / count);
                }
            }
            int[] tmp = src; src = dst; dst = tmp;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int tr = 0, tg = 0, tb = 0, count = 0;
                    for (int yy = Math.max(0, y - radius); yy <= Math.min(h - 1, y + radius); yy++) {
                        int c = src[yy * w + x];
                        tr += (c >>> 16) & 0xFF;
                        tg += (c >>> 8) & 0xFF;
                        tb += c & 0xFF;
                        count++;
                    }
                    dst[y * w + x] = 0xFF000000 | ((tr / count) << 16) | ((tg / count) << 8) | (tb / count);
                }
            }
            tmp = src; src = dst; dst = tmp;
        }
        img.setRGB(0, 0, w, h, src, 0, w);
    }

    private void addDither(BufferedImage img, long seed) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = img.getRGB(x, y);
                int grain = Math.round((noise(x + (int) seed, y - (int) (seed >>> 16)) - 0.5f) * 3f);
                int r = clamp8(((c >>> 16) & 0xFF) + grain);
                int g = clamp8(((c >>> 8) & 0xFF) + grain);
                int b = clamp8((c & 0xFF) + grain);
                img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
    }

    private float[] computeNoiseGradient(float x, float y) {
        float eps = 0.001f;
        float dx = (smoothNoise(x + eps, y) - smoothNoise(x - eps, y)) / (2f * eps);
        float dy = (smoothNoise(x, y + eps) - smoothNoise(x, y - eps)) / (2f * eps);
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.0001f) return new float[]{0f, 0f};
        return new float[]{dx / len, dy / len};
    }

    private float smoothNoise(float x, float y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        float xf = x - x0;
        float yf = y - y0;
        float u = xf * xf * (3f - 2f * xf);
        float v = yf * yf * (3f - 2f * yf);
        float n00 = noise(x0, y0);
        float n10 = noise(x0 + 1, y0);
        float n01 = noise(x0, y0 + 1);
        float n11 = noise(x0 + 1, y0 + 1);
        float nx0 = n00 * (1f - u) + n10 * u;
        float nx1 = n01 * (1f - u) + n11 * u;
        return nx0 * (1f - v) + nx1 * v;
    }

    private float noise(int x, int y) {
        double v = Math.sin(x * 12.9898 + y * 78.233) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    private float randomRange(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private float smoothstep(float edge0, float edge1, float x) {
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3f - 2f * t);
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private float clamp01(float v) {
        return clamp(v, 0f, 1f);
    }

    private int clamp8(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private int mix(int a, int b, float t) {
        t = clamp01(t);
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = clamp8(Math.round(lerp(ar, br, t)));
        int g = clamp8(Math.round(lerp(ag, bg, t)));
        int bl = clamp8(Math.round(lerp(ab, bb, t)));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private void rebuildShader() {
        if (meshPaint != null) {
            meshPaint.close();
            meshPaint = null;
        }
        if (textureShader != null) {
            textureShader.close();
            textureShader = null;
        }
        if (textureImage == null) return;
        textureShader = textureImage.makeShader(
                FilterTileMode.MIRROR,
                FilterTileMode.MIRROR,
                SamplingMode.LINEAR,
                Matrix33.IDENTITY);
        meshPaint = new Paint();
        meshPaint.setAntiAlias(true);
        meshPaint.setShader(textureShader);
    }

    private void closeImage() {
        if (meshPaint != null) {
            meshPaint.close();
            meshPaint = null;
        }
        if (textureShader != null) {
            textureShader.close();
            textureShader = null;
        }
        if (textureImage != null) {
            textureImage.close();
            textureImage = null;
        }
    }

    @Override
    public void close() {
        closeImage();
        texturePixelsABGR = null;
        controlPoints = null;
        controlW = 0;
        controlH = 0;
        gridW = 0;
        gridH = 0;
        vertexCount = 0;
        indices = null;
        colors = null;
        baseUvs = null;
        positions = null;
        texCoords = null;
    }

    public static final class GuiRenderer implements AutoCloseable {
        private static final AtomicLong TEXTURE_ID_COUNTER = new AtomicLong();
        private static final RenderPipeline AMLL_BACKGROUND_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(Identifier.parse("skija-test:pipeline/amll_background"))
                .withFragmentShader(Identifier.parse("skija-test:core/amll_background"))
                .build());
        private final AMLLFluidBackground background = new AMLLFluidBackground();
        private final String name;
        private float[] positions = new float[0];
        private float[] texCoords = new float[0];
        private DynamicTexture texture;
        private Identifier textureId;
        private float lastWidth = -1f;
        private float lastHeight = -1f;
        private boolean ready;

        public GuiRenderer(String name) {
            this.name = name == null || name.isBlank() ? "amll_bg" : name;
        }

        public void rebuild(byte[] coverBytes, String seed) {
            background.rebuild(coverBytes, seed);
            int size = Math.max(0, background.vertexCount() * 2);
            positions = new float[size];
            texCoords = new float[size];
            uploadTexture(background.copyTexturePixelsABGR());
            lastWidth = -1f;
            lastHeight = -1f;
            ready = textureId != null && background.vertexCount() > 0;
        }

        public boolean ready() {
            return ready && textureId != null;
        }

        public boolean draw(GuiGraphicsExtractor g, float width, float height, float time, float lowFreqPulse) {
            if (!ready()) return false;
            int size = background.vertexCount() * 2;
            if (size <= 0) return false;
            if (positions.length < size) positions = new float[size];
            if (texCoords.length < size) texCoords = new float[size];
            if (Math.abs(width - lastWidth) > 0.5f || Math.abs(height - lastHeight) > 0.5f) {
                if (!background.fillGpuPositions(width, height, positions)) return false;
                lastWidth = width;
                lastHeight = height;
            }
            if (!background.fillGpuTexCoords(time, lowFreqPulse, texCoords)) return false;
            if (background.baseUvs == null) return false;
            float volume = Math.max(0f, Math.min(0.45f, lowFreqPulse));
            float alphaVolumeFactor = Math.max(0.5f, 1.0f - volume * 0.5f);
            AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(textureId);
            if (abstractTexture == null || abstractTexture.getTextureView() == null || abstractTexture.getSampler() == null) return false;
            Matrix3x2f pose = new Matrix3x2f(g.pose());
            ScreenRectangle scissor = g.scissorStack.peek();
            ScreenRectangle bounds = new ScreenRectangle(0, 0, Math.max(1, Math.round(width)), Math.max(1, Math.round(height))).transformMaxBounds(pose);
            TextureSetup textureSetup = TextureSetup.singleTexture(abstractTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            g.guiRenderState.addGuiElement(new MeshRenderState(AMLL_BACKGROUND_PIPELINE, textureSetup, pose, positions, texCoords, background.baseUvs, alphaVolumeFactor, background.gridWidth(), background.gridHeight(), width, height, scissor, bounds));
            return true;
        }

        private void uploadTexture(int[] pixelsABGR) {
            closeTexture();
            if (pixelsABGR == null || pixelsABGR.length < TEXTURE_SIZE * TEXTURE_SIZE) return;
            NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, false);
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                for (int x = 0; x < TEXTURE_SIZE; x++) {
                    image.setPixelABGR(x, y, pixelsABGR[y * TEXTURE_SIZE + x]);
                }
            }
            texture = new DynamicTexture(() -> "amll-fluid-bg", image);
            textureId = Identifier.parse("skija-test:" + name + "_" + TEXTURE_ID_COUNTER.incrementAndGet());
            Minecraft.getInstance().getTextureManager().register(textureId, texture);
            texture.upload();
        }

        private void closeTexture() {
            if (textureId != null) {
                try {
                    Minecraft.getInstance().getTextureManager().release(textureId);
                } catch (Exception e) {
                    if (texture != null) {
                        try {
                            texture.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            texture = null;
            textureId = null;
        }

        @Override
        public void close() {
            closeTexture();
            background.close();
            ready = false;
        }

        private record MeshRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
                                       float[] positions, float[] texCoords, float[] baseUvs, float alphaVolumeFactor,
                                       int gridW, int gridH, float width, float height, ScreenRectangle scissorArea, ScreenRectangle bounds)
                implements net.minecraft.client.renderer.state.gui.GuiElementRenderState {
            @Override
            public void buildVertices(VertexConsumer vertexConsumer) {
                for (int y = 0; y < gridH - 1; y++) {
                    for (int x = 0; x < gridW - 1; x++) {
                        int p00 = y * gridW + x;
                        int p10 = p00 + 1;
                        int p01 = (y + 1) * gridW + x;
                        int p11 = p01 + 1;
                        addVertex(vertexConsumer, p00);
                        addVertex(vertexConsumer, p01);
                        addVertex(vertexConsumer, p11);
                        addVertex(vertexConsumer, p10);
                    }
                }
            }

            private void addVertex(VertexConsumer vertexConsumer, int index) {
                int i = index * 2;
                int r = Math.max(0, Math.min(255, Math.round(baseUvs[i] * 255f)));
                int c = Math.max(0, Math.min(255, Math.round(baseUvs[i + 1] * 255f)));
                int b = Math.max(0, Math.min(255, Math.round(alphaVolumeFactor * 255f)));
                vertexConsumer.addVertexWith2DPose(pose, positions[i], positions[i + 1])
                        .setUv(texCoords[i], texCoords[i + 1])
                        .setColor(r, c, b, 255);
            }
        }
    }

    private record PointConf(float x, float y, float ur, float vr, float up, float vp) {}
    private record ControlPoint(float x, float y, float ux, float uy, float vx, float vy) {}
}
