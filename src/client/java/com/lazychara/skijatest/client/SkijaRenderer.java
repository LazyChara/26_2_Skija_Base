package com.lazychara.skijatest.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.types.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class SkijaRenderer implements AutoCloseable {

    private final int width;
    private final int height;
    private Surface surface;
    private DynamicTexture mcTexture;
    private final Identifier textureId;
    private final Typeface defaultTypeface;
    private boolean dirty = true;

    private static final float SQUIRCLE_N = 5f;
    private static final int SQUIRCLE_SEGMENTS = 20;

    @SuppressWarnings("deprecation")
    public SkijaRenderer(String name, int width, int height) {
        this.width = width;
        this.height = height;
        this.surface = Surface.makeRasterN32Premul(width, height);
        NativeImage nativeImage = new NativeImage(width, height, true);
        this.mcTexture = new DynamicTexture(() -> "skija-renderer", nativeImage);
        this.textureId = Identifier.parse("skija-test:" + name);
        Minecraft.getInstance().getTextureManager().register(textureId, mcTexture);
        this.defaultTypeface = FontMgr.getDefault().matchFamilyStyle(null, FontStyle.NORMAL);
    }

    public Canvas canvas() { return surface.getCanvas(); }

    public void clear(int argb) {
        surface.getCanvas().clear(argb);
        dirty = true;
    }

    public void drawRoundedRect(float x, float y, float w, float h, float radius, int argbColor) {
        RRect rrect = RRect.makeLTRB(x, y, x + w, y + h, radius, radius);
        try (Paint paint = new Paint()) {
            paint.setColor(argbColor);
            paint.setAntiAlias(true);
            surface.getCanvas().drawRRect(rrect, paint);
        }
        dirty = true;
    }

    public void drawRoundedRectStroke(float x, float y, float w, float h,
                                       float radius, float strokeWidth, int argbColor) {
        RRect rrect = RRect.makeLTRB(x, y, x + w, y + h, radius, radius);
        try (Paint paint = new Paint()) {
            paint.setColor(argbColor);
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(strokeWidth);
            surface.getCanvas().drawRRect(rrect, paint);
        }
        dirty = true;
    }

    public void drawCircle(float cx, float cy, float radius, int argbColor) {
        try (Paint paint = new Paint()) {
            paint.setColor(argbColor);
            paint.setAntiAlias(true);
            surface.getCanvas().drawCircle(cx, cy, radius, paint);
        }
        dirty = true;
    }

    private static Path makeSquirclePath(float x, float y, float w, float h, float radius) {
        float r = Math.min(radius, Math.min(w, h) / 2f);
        float n = SQUIRCLE_N;
        float exp = 2f / n;
        int seg = SQUIRCLE_SEGMENTS;

        float right = x + w, bottom = y + h;

        PathBuilder pb = new PathBuilder();

        addCorner(pb, right - r, y + r, r, exp, seg, 0, true);
        addCorner(pb, right - r, bottom - r, r, exp, seg, 1, false);
        addCorner(pb, x + r, bottom - r, r, exp, seg, 2, false);
        addCorner(pb, x + r, y + r, r, exp, seg, 3, false);

        pb.closePath();
        return pb.detach();
    }

    private static void addCorner(PathBuilder pb, float cx, float cy, float r,
                                   float exp, int segments, int corner, boolean first) {
        for (int i = 0; i <= segments; i++) {
            float t = (float)(Math.PI / 2.0 * i / segments);
            float cosT = (float) Math.cos(t);
            float sinT = (float) Math.sin(t);
            float sx = Math.signum(cosT) * (float) Math.pow(Math.abs(cosT), exp) * r;
            float sy = Math.signum(sinT) * (float) Math.pow(Math.abs(sinT), exp) * r;

            float px, py;
            switch (corner) {
                case 0: px = cx + sy; py = cy - sx; break;
                case 1: px = cx + sx; py = cy + sy; break;
                case 2: px = cx - sy; py = cy + sx; break;
                case 3: px = cx - sx; py = cy - sy; break;
                default: px = cx; py = cy;
            }

            if (first && i == 0) {
                pb.moveTo(px, py);
                first = false;
            } else {
                pb.lineTo(px, py);
            }
        }
    }

    public void drawSquircle(float x, float y, float w, float h, float radius, int argbColor) {
        try (Path path = makeSquirclePath(x, y, w, h, radius);
             Paint paint = new Paint()) {
            paint.setColor(argbColor);
            paint.setAntiAlias(true);
            surface.getCanvas().drawPath(path, paint);
        }
        dirty = true;
    }

    public void drawSquircleStroke(float x, float y, float w, float h,
                                    float radius, float strokeWidth, int argbColor) {
        try (Path path = makeSquirclePath(x, y, w, h, radius);
             Paint paint = new Paint()) {
            paint.setColor(argbColor);
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(strokeWidth);
            surface.getCanvas().drawPath(path, paint);
        }
        dirty = true;
    }

    public void drawText(String text, float x, float y, float fontSize, int argbColor) {
        if (defaultTypeface == null) return;
        try (Font font = new Font(defaultTypeface, fontSize);
             Paint paint = new Paint()) {
            paint.setColor(argbColor);
            paint.setAntiAlias(true);
            float baseline = y - font.getMetrics().getAscent();
            surface.getCanvas().drawString(text, x, baseline, font, paint);
        }
        dirty = true;
    }

    public void drawText(String text, float x, float y,
                          Typeface typeface, float fontSize, int argbColor) {
        if (typeface == null) { drawText(text, x, y, fontSize, argbColor); return; }
        try (Font font = new Font(typeface, fontSize);
             Paint paint = new Paint()) {
            paint.setColor(argbColor);
            paint.setAntiAlias(true);
            float baseline = y - font.getMetrics().getAscent();
            surface.getCanvas().drawString(text, x, baseline, font, paint);
        }
        dirty = true;
    }

    public void drawTextCentered(String text, float centerX, float y, float fontSize, int argbColor) {
        if (defaultTypeface == null) return;
        try (Font font = new Font(defaultTypeface, fontSize);
             Paint paint = new Paint()) {
            paint.setColor(argbColor);
            paint.setAntiAlias(true);
            float textWidth = font.measureTextWidth(text);
            float baseline = y - font.getMetrics().getAscent();
            surface.getCanvas().drawString(text, centerX - textWidth / 2f, baseline, font, paint);
        }
        dirty = true;
    }

    public void drawTextCentered(String text, float centerX, float y,
                                  Typeface typeface, float fontSize, int argbColor) {
        if (typeface == null) { drawTextCentered(text, centerX, y, fontSize, argbColor); return; }
        try (Font font = new Font(typeface, fontSize);
             Paint paint = new Paint()) {
            paint.setColor(argbColor);
            paint.setAntiAlias(true);
            float textWidth = font.measureTextWidth(text);
            float baseline = y - font.getMetrics().getAscent();
            surface.getCanvas().drawString(text, centerX - textWidth / 2f, baseline, font, paint);
        }
        dirty = true;
    }

    public float measureText(String text, Typeface typeface, float fontSize) {
        Typeface tf = typeface != null ? typeface : defaultTypeface;
        if (tf == null) return 0;
        try (Font font = new Font(tf, fontSize)) {
            return font.measureTextWidth(text);
        }
    }

    public static String[] getSystemFontFamilies() {
        FontMgr mgr = FontMgr.getDefault();
        int count = mgr.getFamiliesCount();
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = mgr.getFamilyName(i);
        }
        return names;
    }

    public static Typeface makeTypeface(String familyName) {
        return FontMgr.getDefault().matchFamilyStyle(familyName, FontStyle.NORMAL);
    }

    public static Typeface loadTypeface(String path) {
        try {
            return FontMgr.getDefault().makeFromFile(path);
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("Failed to load typeface: {}", path, e);
            return null;
        }
    }

    public static Typeface loadTypefaceFromClasspath(String resourcePath) {
        try (var is = SkijaRenderer.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                SkijaTestClient.LOGGER.error("Font resource not found: {}", resourcePath);
                return null;
            }
            byte[] data = is.readAllBytes();
            return FontMgr.getDefault().makeFromData(Data.makeFromBytes(data));
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("Failed to load typeface from classpath: {}", resourcePath, e);
            return null;
        }
    }

    public static java.util.LinkedHashMap<String, Typeface> loadAllBundledFonts() {
        java.util.LinkedHashMap<String, Typeface> fonts = new java.util.LinkedHashMap<>();
        String basePath = "/skija-test/fonts/";

        try {
            var url = SkijaRenderer.class.getResource(basePath);
            if (url == null) {
                SkijaTestClient.LOGGER.warn("[SkijaTest] Font dir not found: {}", basePath);
                return fonts;
            }

            java.nio.file.Path dirPath;
            java.nio.file.FileSystem jarFs = null;

            if ("file".equals(url.getProtocol())) {
                dirPath = java.nio.file.Path.of(url.toURI());
            } else if ("jar".equals(url.getProtocol())) {
                var jarUri = new java.net.URI(url.toString().split("!")[0] + "!/");
                try {
                    jarFs = java.nio.file.FileSystems.getFileSystem(jarUri);
                } catch (Exception e) {
                    jarFs = java.nio.file.FileSystems.newFileSystem(jarUri, java.util.Map.of());
                }
                dirPath = jarFs.getPath(basePath);
            } else {
                return fonts;
            }

            try (var stream = java.nio.file.Files.list(dirPath)) {
                stream.filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".ttf") || name.endsWith(".otf");
                }).sorted().forEach(p -> {
                    String fileName = p.getFileName().toString();
                    String displayName = fileName.replaceFirst("\\.[^.]+$", "");
                    String resourcePath = basePath + fileName;
                    Typeface tf = loadTypefaceFromClasspath(resourcePath);
                    if (tf != null) {
                        fonts.put(displayName, tf);
                        SkijaTestClient.LOGGER.info("[SkijaTest] Loaded font: {}", displayName);
                    }
                });
            }
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[SkijaTest] Failed to scan bundled fonts", e);
        }
        return fonts;
    }

    public void upload() {
        if (!dirty) return;

        try (Image snapshot = surface.makeImageSnapshot()) {
            try (Bitmap bitmap = new Bitmap()) {
                ImageInfo info = ImageInfo.makeN32Premul(width, height);
                bitmap.allocPixels(info);
                snapshot.readPixels(bitmap, 0, 0);

                byte[] pixelBytes = bitmap.readPixels(info, width * 4, 0, 0);

                if (pixelBytes != null) {
                    NativeImage nativeImage = mcTexture.getPixels();
                    for (int row = 0; row < height; row++) {
                        for (int col = 0; col < width; col++) {
                            int off = (row * width + col) * 4;
                            int r = pixelBytes[off] & 0xFF;
                            int g = pixelBytes[off + 1] & 0xFF;
                            int b = pixelBytes[off + 2] & 0xFF;
                            int a = pixelBytes[off + 3] & 0xFF;
                            nativeImage.setPixel(col, row,
                                    (a << 24) | (b << 16) | (g << 8) | r);
                        }
                    }
                    mcTexture.upload();
                }
            }
        }
        dirty = false;
    }

    public Identifier textureId() { return textureId; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Typeface getDefaultTypeface() { return defaultTypeface; }

    @Override
    public void close() {
        if (surface != null) { surface.close(); surface = null; }
        if (mcTexture != null) { mcTexture.close(); mcTexture = null; }
    }
}
