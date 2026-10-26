package com.gathin.tvhome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.tv.TvInputManager;
import android.os.Build;
import android.os.Handler;

/**
 * TvInputManager (API 21) input add/remove notifications. Constructed only behind a version check, so the
 * class and its newer field types are never loaded on Android 4.0, where the Inputs menu falls back to the
 * configured source panel or the TV's own input picker.
 */
@SuppressLint("NewApi")
final class TvInputWatcher {

    private final TvInputManager.TvInputCallback callback;
    private TvInputManager manager;

    TvInputWatcher(Runnable onChange) {
        callback = new TvInputManager.TvInputCallback() {
            @Override
            public void onInputAdded(String inputId) {
                onChange.run();
            }

            @Override
            public void onInputRemoved(String inputId) {
                onChange.run();
            }
        };
    }

    void start(Context context, Handler handler) {
        if (Build.VERSION.SDK_INT < 21) return;
        manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        if (manager != null) {
            try {
                manager.registerCallback(callback, handler);
            } catch (RuntimeException ignored) {
                manager = null;
            }
        }
    }

    void stop() {
        if (manager == null) return;
        try {
            manager.unregisterCallback(callback);
        } catch (RuntimeException ignored) {
        }
        manager = null;
    }
}
