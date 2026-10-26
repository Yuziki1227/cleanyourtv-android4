package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;

/**
 * The typeface behind one of the font_* strings in res/values/fonts.xml, for views built in code. The string
 * points at a bundled font, or, in locales Outfit can't draw (values-vi), names a system family (#833).
 */
final class UiFont {
    private UiFont() {}

    static Typeface get(Context context, int fontString) {
        Resources res = context.getResources();
        TypedValue value = new TypedValue();
        res.getValue(fontString, value, true);
        String name = value.string == null ? "" : String.valueOf(value.string);
        // res/font and Resources.getFont are API 26. values/fonts.xml gives older TVs system family names.
        if (Build.VERSION.SDK_INT >= 26 && name.startsWith("res/")) return res.getFont(value.resourceId);
        return Typeface.create(name.isEmpty() ? "sans-serif" : name, Typeface.NORMAL);
    }
}
