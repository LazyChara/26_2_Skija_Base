package com.lazychara.skijatest.client.Musicpage;

import io.github.humbleui.skija.Typeface;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import com.lazychara.skijatest.client.SkijaRenderer;

final class LyricTextLayout {
    private static final Pattern LYRIC_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final String LYRIC_BREAK_PUNCTUATION = ",.;:!?，。；：！？、）】》」』’”)[\\\\]}>~…";
    private static final float LYRIC_OVERFLOW_PENALTY_MULTIPLIER = 1000f;
    private static final float LYRIC_CJK_BREAK_PENALTY_RATIO = 0.15f;
    private static final float LYRIC_NORMAL_BREAK_PENALTY_RATIO = 0.50f;
    private static final float LYRIC_SPACE_BREAK_REWARD_RATIO = 0.40f;
    private static final float LYRIC_PUNCTUATION_BREAK_REWARD_RATIO = 0.60f;

    static String[] wrapText(String text, Typeface tf, float size, float maxWidth, SkijaRenderer renderer) {
        String normalized = normalizeLyricText(text);
        if (normalized.isEmpty()) return new String[]{""};
        if (renderer.measureText(normalized, tf, size) <= maxWidth) return new String[]{normalized};
        List<TextSegment> segments = lyricTextSegments(normalized, tf, size, renderer);
        List<Integer> breaks = balancedLyricBreaks(segments, maxWidth);
        if (breaks.isEmpty()) return new String[]{normalized};
        Set<Integer> breakSet = new HashSet<>(breaks);
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (breakSet.contains(i) && !cur.isEmpty()) {
                String line = trimLyricDrawLine(cur.toString());
                if (!line.isEmpty()) lines.add(line);
                cur.setLength(0);
            }
            cur.append(segments.get(i).text());
        }
        String last = trimLyricDrawLine(cur.toString());
        if (!last.isEmpty()) lines.add(last);
        return lines.isEmpty() ? new String[]{normalized} : lines.toArray(String[]::new);
    }

    static float fitLyricTextSize(String text, Typeface tf, float preferredSize, float minSize, float maxWidth, SkijaRenderer renderer, float amllLyricTextMaxWidth) {
        String normalized = normalizeLyricText(text);
        float size = preferredSize;
        while (size > minSize && longestLyricSegmentWidth(normalized, tf, size, renderer) > amllLyricTextMaxWidth) {
            size -= 1.4f;
        }
        return Math.max(minSize, size);
    }

    private static float longestLyricSegmentWidth(String text, Typeface tf, float size, SkijaRenderer renderer) {
        String normalized = normalizeLyricText(text);
        if (normalized.isEmpty()) return 0f;
        float result = 0f;
        for (TextSegment segment : lyricTextSegments(normalized, tf, size, renderer)) {
            if (!segment.isSpace()) result = Math.max(result, segment.width());
        }
        return result;
    }

    static String normalizeLyricText(String text) {
        if (text == null) return "";
        return LYRIC_SPACE_PATTERN.matcher(text.strip()).replaceAll(" ");
    }

    private static String trimLyricDrawLine(String text) {
        if (text == null) return "";
        return text.strip();
    }

    private static List<TextSegment> lyricTextSegments(String text, Typeface tf, float size, SkijaRenderer renderer) {
        ArrayList<TextSegment> result = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String part = text.substring(start, end);
            if (part.isEmpty()) continue;
            appendLyricTextSegment(result, part, tf, size, renderer);
        }
        if (result.isEmpty()) result.add(new TextSegment(text, renderer.measureText(text, tf, size), text.isBlank()));
        return result;
    }

    private static void appendLyricTextSegment(List<TextSegment> result, String part, Typeface tf, float size, SkijaRenderer renderer) {
        if (part.isBlank()) {
            result.add(new TextSegment(part, renderer.measureText(part, tf, size), true));
            return;
        }
        if (!containsCjk(part)) {
            result.add(new TextSegment(part, renderer.measureText(part, tf, size), false));
            return;
        }
        StringBuilder pending = new StringBuilder();
        for (int offset = 0; offset < part.length();) {
            int cp = part.codePointAt(offset);
            int len = Character.charCount(cp);
            String unit = part.substring(offset, offset + len);
            if (isCjkCodePoint(cp)) {
                if (!pending.isEmpty()) {
                    String pendingText = pending.toString();
                    result.add(new TextSegment(pendingText, renderer.measureText(pendingText, tf, size), false));
                    pending.setLength(0);
                }
                result.add(new TextSegment(unit, renderer.measureText(unit, tf, size), false));
            } else {
                pending.append(unit);
            }
            offset += len;
        }
        if (!pending.isEmpty()) {
            String pendingText = pending.toString();
            result.add(new TextSegment(pendingText, renderer.measureText(pendingText, tf, size), false));
        }
    }

    private static List<Integer> balancedLyricBreaks(List<TextSegment> segments, float maxWidth) {
        int n = segments.size();
        ArrayList<Integer> result = new ArrayList<>();
        if (n <= 1 || maxWidth <= 1f) return result;
        double[] prefixWidth = new double[n + 1];
        for (int i = 0; i < n; i++) prefixWidth[i + 1] = prefixWidth[i] + segments.get(i).width();
        if (prefixWidth[n] <= maxWidth) return result;
        double[] dp = new double[n + 1];
        int[] nextBreak = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            dp[i] = Double.POSITIVE_INFINITY;
            nextBreak[i] = -1;
        }
        dp[n] = 0.0;
        double cjkPenalty = Math.pow(maxWidth * LYRIC_CJK_BREAK_PENALTY_RATIO, 2.0);
        double normalPenalty = Math.pow(maxWidth * LYRIC_NORMAL_BREAK_PENALTY_RATIO, 2.0);
        for (int i = n - 1; i >= 0; i--) {
            for (int j = i + 1; j <= n; j++) {
                double lineW = prefixWidth[j] - prefixWidth[i];
                double lineCost;
                if (lineW > maxWidth) {
                    if (j == i + 1) lineCost = Math.pow(lineW - maxWidth, 2.0) * LYRIC_OVERFLOW_PENALTY_MULTIPLIER;
                    else continue;
                } else {
                    lineCost = Math.pow(maxWidth - lineW, 2.0);
                }
                double breakPenalty = 0.0;
                if (j < n) {
                    TextSegment prev = segments.get(j - 1);
                    if (endsWithLyricPunctuation(prev.text())) breakPenalty = -Math.pow(maxWidth * LYRIC_PUNCTUATION_BREAK_REWARD_RATIO, 2.0);
                    else if (prev.isSpace()) breakPenalty = -Math.pow(maxWidth * LYRIC_SPACE_BREAK_REWARD_RATIO, 2.0);
                    else if (isCjkBreakBoundary(segments, j)) breakPenalty = cjkPenalty;
                    else breakPenalty = normalPenalty;
                }
                double total = lineCost + breakPenalty + dp[j];
                if (total < dp[i]) {
                    dp[i] = total;
                    nextBreak[i] = j;
                }
            }
        }
        int cur = 0;
        while (cur < n) {
            int next = nextBreak[cur];
            if (next <= cur || next > n) break;
            if (next < n) result.add(next);
            cur = next;
        }
        return result;
    }

    private static boolean endsWithLyricPunctuation(String text) {
        if (text == null || text.isEmpty()) return false;
        String stripped = text.stripTrailing();
        if (stripped.isEmpty()) return false;
        int cp = stripped.codePointBefore(stripped.length());
        return LYRIC_BREAK_PUNCTUATION.indexOf(cp) >= 0;
    }

    private static boolean isCjkBreakBoundary(List<TextSegment> segments, int index) {
        if (index <= 0 || index >= segments.size()) return false;
        return containsCjk(segments.get(index - 1).text()) || containsCjk(segments.get(index).text());
    }

    static boolean containsCjk(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            if (isCjkCodePoint(cp)) return true;
            offset += Character.charCount(cp);
        }
        return false;
    }

    static boolean isCjkCodePoint(int cp) {
        return Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN || (cp >= 0x0800 && cp <= 0x9FFC);
    }

    static java.util.List<LyricWordPlacement> measureLyricWordPlacements(LyricLine line, Typeface tf, float size, String[] mainLines, float linePadX, float maxLineW, float textMaxWidth, SkijaRenderer renderer) {
        ArrayList<LyricWordPlacement> result = new ArrayList<>();
        if (line == null || line.words() == null || line.words().isEmpty()) return result;
        String normalized = normalizeLyricText(line.text());
        int[] lineStarts = wrappedLineStarts(normalized, mainLines);
        int[] lineEnds = new int[lineStarts.length];
        for (int i = 0; i < lineStarts.length; i++) lineEnds[i] = Math.min(normalized.length(), lineStarts[i] + (mainLines[i] == null ? 0 : mainLines[i].length()));
        int cursor = 0;
        int fallbackLineIndex = 0;
        float fallbackX = lyricLineTextStartX(line, linePadX, maxLineW, mainLines.length > 0 ? renderer.measureText(mainLines[0], tf, size) : textMaxWidth);
        for (LyricWord word : line.words()) {
            String raw = word.word() == null ? "" : word.word();
            String search = normalizeLyricText(raw);
            LyricWordPlacement placement = null;
            if (!search.isEmpty() && !normalized.isEmpty() && mainLines.length > 0) {
                int from = Math.max(0, Math.min(cursor, normalized.length()));
                int start = normalized.indexOf(search, from);
                if (start < 0 && from > 0) start = normalized.indexOf(search);
                if (start >= 0) {
                    int end = Math.min(normalized.length(), start + search.length());
                    int lineIndex = lyricLineIndexForCharRange(lineStarts, lineEnds, start, end);
                    if (lineIndex >= 0 && lineIndex < mainLines.length) {
                        String lineText = mainLines[lineIndex] == null ? "" : mainLines[lineIndex];
                        int localStart = Math.max(0, Math.min(lineText.length(), start - lineStarts[lineIndex]));
                        int localEnd = Math.max(localStart, Math.min(lineText.length(), end - lineStarts[lineIndex]));
                        float lineW = renderer.measureText(lineText, tf, size);
                        float lineStartX = lyricLineTextStartX(line, linePadX, maxLineW, lineW);
                        float startX = lineStartX + renderer.measureText(lineText.substring(0, localStart), tf, size);
                        float endX = lineStartX + renderer.measureText(lineText.substring(0, localEnd), tf, size);
                        placement = new LyricWordPlacement(lineIndex, startX, Math.max(startX, endX), lineText.substring(localStart, localEnd));
                        cursor = end;
                    }
                }
            }
            if (placement == null) {
                String text = search.isEmpty() ? raw : search;
                float wordW = renderer.measureText(text, tf, size);
                fallbackLineIndex = Math.max(0, Math.min(mainLines.length - 1, fallbackLineIndex));
                String fallbackLine = mainLines.length > 0 ? mainLines[fallbackLineIndex] : "";
                float fallbackLineW = renderer.measureText(fallbackLine, tf, size);
                float fallbackLineStartX = lyricLineTextStartX(line, linePadX, maxLineW, fallbackLineW);
                float fallbackLineEndX = fallbackLineStartX + fallbackLineW;
                if (fallbackX > fallbackLineStartX && fallbackX + wordW > fallbackLineEndX + 0.5f && fallbackLineIndex + 1 < mainLines.length) {
                    fallbackLineIndex++;
                    fallbackLine = mainLines[fallbackLineIndex];
                    fallbackLineW = renderer.measureText(fallbackLine, tf, size);
                    fallbackLineStartX = lyricLineTextStartX(line, linePadX, maxLineW, fallbackLineW);
                    fallbackX = fallbackLineStartX;
                }
                placement = new LyricWordPlacement(fallbackLineIndex, fallbackX, fallbackX + Math.max(0f, wordW), text);
            }
            result.add(placement);
            fallbackLineIndex = placement.lineIndex();
            fallbackX = placement.endX();
        }
        return result;
    }

    static float lyricLineTextStartX(LyricLine line, float linePadX, float maxLineW, float lineW) {
        return linePadX + (line != null && line.isDuet() ? Math.max(0f, maxLineW - lineW) : 0f);
    }

    private static int[] wrappedLineStarts(String normalized, String[] mainLines) {
        if (mainLines == null) return new int[0];
        int[] result = new int[mainLines.length];
        int cursor = 0;
        String source = normalized == null ? "" : normalized;
        for (int i = 0; i < mainLines.length; i++) {
            String line = mainLines[i] == null ? "" : mainLines[i];
            int from = Math.max(0, Math.min(cursor, source.length()));
            int start = line.isEmpty() ? from : source.indexOf(line, from);
            if (start < 0) start = from;
            result[i] = Math.max(0, Math.min(start, source.length()));
            cursor = Math.min(source.length(), result[i] + line.length());
            while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) cursor++;
        }
        return result;
    }

    private static int lyricLineIndexForCharRange(int[] starts, int[] ends, int start, int end) {
        if (starts.length == 0) return 0;
        for (int i = 0; i < starts.length; i++) {
            if (start >= starts[i] && start < ends[i]) return i;
        }
        for (int i = 0; i < starts.length; i++) {
            if (end > starts[i] && start < ends[i]) return i;
        }
        return starts.length - 1;
    }
}
