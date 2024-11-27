package com.epam.aidial.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TextUtils {
    public String truncateText(String text, int maxLines, int maxChars) {
        int stop = Math.min(maxChars, text.length());
        int lineCount = 0;
        boolean isLastLineEmpty = true;
        int lastLineStart = text.length();
        int lastPosition = lastLineStart;
        for (int i = 0; i < stop; ++i, --lastPosition) {
            char c = text.charAt(lastPosition - 1);
            if (c == '\n') {
                if (!isLastLineEmpty) {
                    isLastLineEmpty = true;
                    lastLineStart = lastPosition;
                    if (++lineCount >= maxLines) {
                        break;
                    }
                }
            } else if (c != '\r') {
                isLastLineEmpty = false;
            }
        }

        if (lastPosition == 0 && !isLastLineEmpty) {
            return text;
        }

        return text.substring(lastLineStart);
    }
}
