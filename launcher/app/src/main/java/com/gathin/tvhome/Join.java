package com.gathin.tvhome;

/**
 * String.join is API 26. This is the same join for the launcher's small lists, and it has no Android types, so the
 * JVM tests can use the same code paths as the TV.
 */
final class Join {
    private Join() {
    }

    static String of(String separator, Iterable<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(separator);
            out.append(value);
        }
        return out.toString();
    }
}
