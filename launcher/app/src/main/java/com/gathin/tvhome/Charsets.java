package com.gathin.tvhome;

import java.nio.charset.Charset;

/**
 * java.nio.charset.StandardCharsets is API 19. The same charsets exist since Android 1.0 through
 * Charset.forName, so the launcher's small text files can use one constant on every TV.
 */
final class Charsets {
    static final Charset UTF_8 = Charset.forName("UTF-8");
    static final Charset US_ASCII = Charset.forName("US-ASCII");

    private Charsets() {
    }
}
