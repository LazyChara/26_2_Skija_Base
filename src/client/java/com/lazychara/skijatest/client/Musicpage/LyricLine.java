package com.lazychara.skijatest.client.Musicpage;

import java.util.List;

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


