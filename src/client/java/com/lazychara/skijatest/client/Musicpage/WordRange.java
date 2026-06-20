package com.lazychara.skijatest.client.Musicpage;

import java.util.List;

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


