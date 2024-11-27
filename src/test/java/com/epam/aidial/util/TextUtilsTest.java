package com.epam.aidial.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {
    @Test
    void testEmptyLinesOnlyAreIgnored() {
        String input = "\r\n\r\n\r\n";

        String output = TextUtils.truncateText(input, 0, input.length());

        assertThat(output).isEmpty();
    }

    @Test
    void testEmptyLinesAfterAreNotCountedAsLines() {
        String input = "\n\r\na\n\r\nb\n\n";

        String output = TextUtils.truncateText(input, 2, input.length());

        assertThat(output).isEqualTo("a\n\r\nb\n\n");
    }

    @Test
    void testMaxLinesWithoutLastLinebreak() {
        String input = "a\nb\nc";

        String output = TextUtils.truncateText(input, 2, input.length());

        assertThat(output).isEqualTo("b\nc");
    }

    @Test
    void testMaxLinesNotExceeded() {
        String input = "a\nb\nc\n";

        String output = TextUtils.truncateText(input, 3, input.length());

        assertThat(output).isEqualTo(input);
    }

    @Test
    void testMaxChars() {
        String input = "abc\ndef\nghi\n";

        String output = TextUtils.truncateText(input, 3, input.length() - 1);

        assertThat(output).isEqualTo("def\nghi\n");
    }
}