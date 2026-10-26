package com.gathin.tvhome;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.Locale;

/**
 * Small bridges for APIs that exist on the modern TVs this launcher was written for but not on Android 4.0.
 * Every method is a no-op (or a sensible older equivalent) below the API that introduced the real one, so the
 * rest of the launcher can keep one code path.
 */
final class Compat {

    /** View.generateViewId() is API 17; a process-local counter is enough for ids used only inside a process. */
    private static int nextViewId = 1;

    private Compat() {
    }

    static boolean atLeast(int sdk) {
        return Build.VERSION.SDK_INT >= sdk;
    }

    /** Integer/Long/Boolean.compare are API 19; these are the same, for Android 4.0. */
    static int compare(int a, int b) {
        return a < b ? -1 : a > b ? 1 : 0;
    }

    static int compare(long a, long b) {
        return a < b ? -1 : a > b ? 1 : 0;
    }

    static int compare(boolean a, boolean b) {
        return a == b ? 0 : a ? 1 : -1;
    }

    // ---- View -------------------------------------------------------------------------------------

    static void setBackground(View view, Drawable drawable) {
        if (atLeast(16)) view.setBackground(drawable);
        else view.setBackgroundDrawable(drawable);
    }

    static void setElevation(View view, float elevation) {
        if (atLeast(21)) view.setElevation(elevation);
    }

    static void setElevation(PopupWindow popup, float elevation) {
        if (atLeast(21)) popup.setElevation(elevation);
    }

    static void setTranslationZ(View view, float translationZ) {
        if (atLeast(21)) view.setTranslationZ(translationZ);
    }

    static void setClipToOutline(View view, boolean clip) {
        if (atLeast(21)) view.setClipToOutline(clip);
    }

    static void setImportantForAccessibility(View view, int mode) {
        if (atLeast(16)) view.setImportantForAccessibility(mode);
    }

    static void setLayoutDirection(View view, int direction) {
        if (atLeast(17)) view.setLayoutDirection(direction);
    }

    static void setLetterSpacing(TextView view, float spacing) {
        if (atLeast(21)) view.setLetterSpacing(spacing);
    }

    static void setPaddingRelative(View view, int start, int top, int end, int bottom) {
        if (atLeast(17)) view.setPaddingRelative(start, top, end, bottom);
        else view.setPadding(start, top, end, bottom);
    }

    static void setCompoundDrawablesRelative(TextView view, Drawable left, Drawable top, Drawable right,
                                             Drawable bottom) {
        if (atLeast(17)) view.setCompoundDrawablesRelative(left, top, right, bottom);
        else view.setCompoundDrawables(left, top, right, bottom);
    }

    static void setCompoundDrawablesRelativeWithIntrinsicBounds(TextView view, int left, int top, int right,
                                                                int bottom) {
        if (atLeast(17)) view.setCompoundDrawablesRelativeWithIntrinsicBounds(left, top, right, bottom);
        else view.setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
    }

    static void setCompoundDrawablesRelativeWithIntrinsicBounds(TextView view, Drawable left, Drawable top,
                                                                Drawable right, Drawable bottom) {
        if (atLeast(17)) view.setCompoundDrawablesRelativeWithIntrinsicBounds(left, top, right, bottom);
        else view.setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
    }

    static Drawable[] getCompoundDrawablesRelative(TextView view) {
        return atLeast(17) ? view.getCompoundDrawablesRelative() : view.getCompoundDrawables();
    }

    static int getPaddingStart(View view) {
        return atLeast(17) ? view.getPaddingStart() : view.getPaddingLeft();
    }

    static int getPaddingEnd(View view) {
        return atLeast(17) ? view.getPaddingEnd() : view.getPaddingRight();
    }

    /** TextView.getMaxWidth is API 16; use the measured width (or a quarter of the screen early on) below it. */
    static int getMaxWidth(TextView view) {
        if (atLeast(16)) return view.getMaxWidth();
        int width = view.getWidth();
        return width > 0 ? width : view.getResources().getDisplayMetrics().widthPixels / 4;
    }

    /** MarginLayoutParams.setMarginEnd is API 17; on older LTR layouts the end is the right edge. */
    static void setMarginEnd(ViewGroup.MarginLayoutParams params, int pixels) {
        if (atLeast(17)) params.setMarginEnd(pixels);
        else params.rightMargin = pixels;
    }

    static void setMarginStart(ViewGroup.MarginLayoutParams params, int pixels) {
        if (atLeast(17)) params.setMarginStart(pixels);
        else params.leftMargin = pixels;
    }

    static int generateViewId() {
        if (atLeast(17)) return View.generateViewId();
        int id;
        do {
            id = nextViewId++;
        } while (id == View.NO_ID);
        return id;
    }

    static void postOnAnimation(View view, Runnable action) {
        if (atLeast(16)) view.postOnAnimation(action);
        else view.postDelayed(action, 16);
    }

    static void postInvalidateOnAnimation(View view) {
        if (atLeast(16)) view.postInvalidateOnAnimation();
        else view.postInvalidate();
    }

    static boolean isLaidOut(View view) {
        if (atLeast(19)) return view.isLaidOut();
        return view.getWidth() > 0 && view.getHeight() > 0;
    }

    static void removeOnGlobalLayoutListener(ViewTreeObserver observer,
                                             ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (atLeast(16)) observer.removeOnGlobalLayoutListener(listener);
        else observer.removeGlobalOnLayoutListener(listener);   // the same call, before API 16
    }

    /** TextView.getCompoundDrawableTintList is API 23; older views have no tint list to compare. */
    static ColorStateList getCompoundDrawableTintList(TextView view) {
        return atLeast(23) ? view.getCompoundDrawableTintList() : null;
    }

    static void forceHasOverlappingRendering(View view, boolean force) {
        if (atLeast(24)) view.forceHasOverlappingRendering(force);
    }

    static boolean isDestroyed(Activity activity) {
        return atLeast(17) ? activity.isDestroyed() : activity.isFinishing();
    }

    // ---- animation --------------------------------------------------------------------------------

    static ViewPropertyAnimator withLayer(ViewPropertyAnimator animator) {
        if (atLeast(16)) animator.withLayer();
        return animator;
    }

    static ViewPropertyAnimator withEndAction(ViewPropertyAnimator animator, Runnable action) {
        if (atLeast(16)) return animator.withEndAction(action);
        // ViewPropertyAnimator.withEndAction is API 16. A listener is the API 1 equivalent; it replaces any
        // listener these short UI animations already had (none of the call sites set one).
        animator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                action.run();
            }
        });
        return animator;
    }

    /** Canvas.saveLayerAlpha's int-coordinate overload is API 21; saveLayer(RectF, Paint, flags) is API 1. */
    private static final RectF LAYER_BOUNDS = new RectF();
    private static final Paint LAYER_PAINT = new Paint();

    static int saveLayerAlpha(Canvas canvas, Rect bounds, int alpha) {
        if (alpha >= 255) return canvas.save();
        LAYER_BOUNDS.set(bounds);
        LAYER_PAINT.setAlpha(alpha);
        return canvas.saveLayer(LAYER_BOUNDS, LAYER_PAINT, Canvas.ALL_SAVE_FLAG);
    }

    // ---- resources --------------------------------------------------------------------------------

    static Drawable getDrawable(Context context, int id) {
        return context.getResources().getDrawable(id);
    }

    static ColorStateList getColorStateList(Context context, int id) {
        return context.getResources().getColorStateList(id);
    }

    /** Drawable.setTint is API 21; a SRC_IN colour filter is the equivalent on older platforms. */
    static void tint(Drawable drawable, int color) {
        if (drawable == null) return;
        if (atLeast(21)) drawable.setTint(color);
        else drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    /** TextView.setCompoundDrawableTintList is API 23; tint the current compound drawables instead. */
    static void setCompoundDrawableTintList(TextView view, ColorStateList tint) {
        if (atLeast(23)) {
            view.setCompoundDrawableTintList(tint);
            return;
        }
        int color = tint == null ? Color.WHITE : tint.getDefaultColor();
        for (Drawable drawable : getCompoundDrawablesRelative(view)) {
            if (drawable == null) continue;
            if (tint == null) drawable.clearColorFilter();
            else drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    // ---- platform services ------------------------------------------------------------------------

    /** PackageManager.getLeanbackLaunchIntentForPackage is API 21; on older sets the normal launcher intent is all there is. */
    static Intent leanbackOrLaunch(PackageManager pm, String packageName) {
        if (atLeast(21)) {
            Intent leanback = pm.getLeanbackLaunchIntentForPackage(packageName);
            if (leanback != null) return leanback;
        }
        return pm.getLaunchIntentForPackage(packageName);
    }

    /** Configuration.getLocales is API 24. */
    @SuppressWarnings("deprecation")
    static Locale primaryLocale(Configuration configuration) {
        if (atLeast(24)) return configuration.getLocales().get(0);
        return configuration.locale != null ? configuration.locale : Locale.getDefault();
    }

    /** Locale.forLanguageTag is API 21. A plain tag parses the same way for the languages a TV offers. */
    static Locale forLanguageTag(String tag) {
        // SDK 0 is the JVM test runner: java.util.Locale is complete there, including script/extensions.
        if (atLeast(21) || Build.VERSION.SDK_INT == 0) return Locale.forLanguageTag(tag);
        String[] parts = tag.split("-");
        if (parts.length == 0 || parts[0].isEmpty()) return Locale.getDefault();
        String language = parts[0];
        String country = "";
        String script = "";
        if (parts.length > 1) {
            if (parts[1].length() == 4) script = parts[1];
            else if (parts[1].length() == 2 || parts[1].length() == 3) country = parts[1];
        }
        return new Locale(language, country, script);
    }

    /** Context.createConfigurationContext and Configuration.setLocale are API 17. On 14-16 the app's
     *  resources are updated in place, which is the only locale switch those releases have. */
    @SuppressWarnings("deprecation")
    static Context wrapLocale(Context base, Locale locale) {
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        if (atLeast(17)) {
            configuration.setLocale(locale);
            return base.createConfigurationContext(configuration);
        }
        configuration.locale = locale;
        base.getResources().updateConfiguration(configuration, base.getResources().getDisplayMetrics());
        return base;
    }

    /** PowerManager.isInteractive is API 20. */
    @SuppressWarnings("deprecation")
    static boolean isScreenOn(PowerManager power) {
        if (power == null) return true;
        return atLeast(20) ? power.isInteractive() : power.isScreenOn();
    }

    /** Context.checkSelfPermission is API 23; before that every normal permission was granted at install. */
    static boolean checkSelfPermission(Context context, String permission) {
        if (atLeast(23)) return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        return context.getPackageManager().checkPermission(permission, context.getPackageName())
                == PackageManager.PERMISSION_GRANTED;
    }

    static String permissionWriteSecureSettings() {
        return Manifest.permission.WRITE_SECURE_SETTINGS;
    }
}
