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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

public final class AMLLFluidBackground implements AutoCloseable {
    private static final int TEXTURE_SIZE = 32;
    private static final int CONTROL_W = 5;
    private static final int CONTROL_H = 5;
    private static final int SUBDIVISIONS = 8;
    private static final int GRID_W = (CONTROL_W - 1) * SUBDIVISIONS + 1;
    private static final int GRID_H = (CONTROL_H - 1) * SUBDIVISIONS + 1;
    private static final int VERTEX_COUNT = GRID_W * GRID_H;

    private Image textureImage;
    private Shader textureShader;
    private Paint meshPaint;
    private ControlPoint[][] controlPoints;
    private short[] indices;
    private int[] colors;
    private float[] baseUvs;
    private Point[] positions;
    private Point[] texCoords;
    private float lastWidth = -1f;
    private float lastHeight = -1f;
    private long seed;

    public Image image() {
        return textureImage;
    }

    public void rebuild(byte[] coverBytes, String seedText) {
        rebuild(coverBytes, seedText, 0, 0);
    }

    public void rebuild(byte[] coverBytes, String seedText, int targetWidth, int targetHeight) {
        closeImage();
        try {
            seed = seedText == null ? 0L : seedText.hashCode();
            BufferedImage cover = null;
            if (coverBytes != null && coverBytes.length > 0) {
                cover = ImageIO.read(new ByteArrayInputStream(coverBytes));
            }
            BufferedImage texture = cover != null ? makeAlbumTexture(cover) : makeFallbackTexture(seed);
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
            controlPoints = null;
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
        PointConf[][] conf = new PointConf[CONTROL_H][CONTROL_W];
        float dx = 2f / (CONTROL_W - 1);
        float dy = 2f / (CONTROL_H - 1);
        float variationFraction = randomRange(random, 0.40f, 0.58f);
        float normalOffset = randomRange(random, 0.30f, 0.52f);
        float smoothFactor = randomRange(random, 0.20f, 0.30f);
        float smoothModifier = randomRange(random, -0.10f, -0.05f);
        int smoothIters = 3 + random.nextInt(2);

        for (int y = 0; y < CONTROL_H; y++) {
            for (int x = 0; x < CONTROL_W; x++) {
                float baseX = (x / (float) (CONTROL_W - 1)) * 2f - 1f;
                float baseY = (y / (float) (CONTROL_H - 1)) * 2f - 1f;
                boolean border = x == 0 || x == CONTROL_W - 1 || y == 0 || y == CONTROL_H - 1;
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
                    px += gradient[0] * normalOffset * weight * 0.8f;
                    py += gradient[1] * normalOffset * weight * 0.8f;
                }
                float ur = border ? 0f : randomRange(random, -60f, 60f);
                float vr = border ? 0f : randomRange(random, -60f, 60f);
                float up = border ? 1f : randomRange(random, 0.80f, 1.20f);
                float vp = border ? 1f : randomRange(random, 0.80f, 1.20f);
                conf[y][x] = new PointConf(px, py, ur, vr, up, vp);
            }
        }

        smoothifyControlPoints(conf, smoothIters, smoothFactor, smoothModifier);

        ControlPoint[][] out = new ControlPoint[CONTROL_H][CONTROL_W];
        float uPower = 2f / (CONTROL_W - 1);
        float vPower = 2f / (CONTROL_H - 1);
        for (int y = 0; y < CONTROL_H; y++) {
            for (int x = 0; x < CONTROL_W; x++) {
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
        float[][] kernel = {
                {1f, 2f, 1f},
                {2f, 4f, 2f},
                {1f, 2f, 1f}
        };
        for (int iter = 0; iter < iterations; iter++) {
            PointConf[][] next = new PointConf[CONTROL_H][CONTROL_W];
            for (int y = 0; y < CONTROL_H; y++) {
                for (int x = 0; x < CONTROL_W; x++) {
                    if (x == 0 || x == CONTROL_W - 1 || y == 0 || y == CONTROL_H - 1) {
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
            for (int y = 0; y < CONTROL_H; y++) {
                System.arraycopy(next[y], 0, grid[y], 0, CONTROL_W);
            }
            f = clamp01(f + modifier);
        }
    }

    private void buildStaticMeshData() {
        indices = new short[(GRID_W - 1) * (GRID_H - 1) * 6];
        int idx = 0;
        for (int y = 0; y < GRID_H - 1; y++) {
            for (int x = 0; x < GRID_W - 1; x++) {
                int p00 = y * GRID_W + x;
                int p10 = y * GRID_W + x + 1;
                int p01 = (y + 1) * GRID_W + x;
                int p11 = (y + 1) * GRID_W + x + 1;
                indices[idx++] = (short) p00;
                indices[idx++] = (short) p10;
                indices[idx++] = (short) p01;
                indices[idx++] = (short) p10;
                indices[idx++] = (short) p11;
                indices[idx++] = (short) p01;
            }
        }
        colors = new int[VERTEX_COUNT];
        baseUvs = new float[VERTEX_COUNT * 2];
        for (int y = 0; y < GRID_H; y++) {
            for (int x = 0; x < GRID_W; x++) {
                int i = y * GRID_W + x;
                colors[i] = 0xFFFFFFFF;
                baseUvs[i * 2] = x / (float) (GRID_W - 1);
                baseUvs[i * 2 + 1] = y / (float) (GRID_H - 1);
            }
        }
    }

    private void ensurePositions(float width, float height) {
        if (positions != null && Math.abs(lastWidth - width) < 0.5f && Math.abs(lastHeight - height) < 0.5f) return;
        positions = new Point[VERTEX_COUNT];
        float aspect = Math.max(0.01f, width / Math.max(1f, height));
        for (int gy = 0; gy < GRID_H; gy++) {
            int cellY = Math.min(CONTROL_H - 2, gy / SUBDIVISIONS);
            float v = (gy - cellY * SUBDIVISIONS) / (float) SUBDIVISIONS;
            for (int gx = 0; gx < GRID_W; gx++) {
                int cellX = Math.min(CONTROL_W - 2, gx / SUBDIVISIONS);
                float u = (gx - cellX * SUBDIVISIONS) / (float) SUBDIVISIONS;
                float[] p = evalPatch(cellX, cellY, u, v);
                float px = p[0];
                float py = p[1];
                if (aspect > 1f) py *= aspect;
                else px /= aspect;
                positions[gy * GRID_W + gx] = new Point((px + 1f) * 0.5f * width, (py + 1f) * 0.5f * height);
            }
        }
        lastWidth = width;
        lastHeight = height;
    }

    private void updateTextureCoordinates(float time, float lowFreqVolume) {
        if (texCoords == null || texCoords.length != VERTEX_COUNT) {
            texCoords = new Point[VERTEX_COUNT];
        }
        float volume = clamp(lowFreqVolume, 0f, 0.45f);
        float uTime = time * 0.24f;
        float angle = (uTime + volume) * 2.0f;
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float scale = Math.max(0.62f, 1.0f - volume * 2.0f);
        float texMax = TEXTURE_SIZE - 1f;
        for (int i = 0; i < VERTEX_COUNT; i++) {
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
        controlPoints = null;
        indices = null;
        colors = null;
        baseUvs = null;
        positions = null;
        texCoords = null;
    }

    private record PointConf(float x, float y, float ur, float vr, float up, float vp) {}
    private record ControlPoint(float x, float y, float ux, float uy, float vx, float vy) {}
}
