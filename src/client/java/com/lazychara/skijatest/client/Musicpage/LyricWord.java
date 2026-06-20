package com.lazychara.skijatest.client.Musicpage;

import java.util.List;

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


