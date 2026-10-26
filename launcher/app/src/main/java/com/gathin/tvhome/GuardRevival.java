package com.gathin.tvhome;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Starts this launcher's process after the firmware has killed it.
 *
 * A notification listener keeps its approval across a force stop, and the system binds it again
 * once the process is gone. That rebind is what brings Home back on TCL and Xiaomi. No notification
 * is read. Enable with {@code cmd notification allow_listener com.gathin.tvhome/.GuardRevival}.
 */
@SuppressLint("NewApi")
public class GuardRevival extends NotificationListenerService {
    private BroadcastReceiver unlocked;

    @Override
    public void onListenerConnected() {
        // UserManager is API 24; the listener itself is API 18 and this is a no-op before 24.
        UserManager users = Compat.atLeast(24) ? (UserManager) getSystemService(Context.USER_SERVICE) : null;
        if (users != null && !users.isUserUnlocked()) {
            if (unlocked != null) return;
            unlocked = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    stopWaiting();
                    GuardKeep.ensureHomeKey(GuardRevival.this);
                }
            };
            registerReceiver(unlocked, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
            if (users.isUserUnlocked()) {
                stopWaiting();
                GuardKeep.ensureHomeKey(this);
            }
            return;
        }
        GuardKeep.ensureHomeKey(this);
    }

    // These two were abstract before API 21; the listener only brings the process back, it never reads a
    // notification, so empty overrides are the whole job (and make the service instantiable on API 18-20).
    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
    }

    private void stopWaiting() {
        if (unlocked == null) return;
        try {
            unregisterReceiver(unlocked);
        } catch (RuntimeException ignored) {
        }
        unlocked = null;
    }

    @Override
    public void onDestroy() {
        stopWaiting();
        super.onDestroy();
    }
}
