package com.gathin.tvhome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/**
 * Watches the network on API 21+, where ConnectivityManager.NetworkCallback exists. The class is only
 * constructed behind a version check, so Android 4.0 never loads the newer types. On API 24+ the default
 * network callback is used; on 21-23 an internet request stands in for it.
 */
@SuppressLint("NewApi")
final class NetworkWatcher {

    interface Listener {
        void onNetworkChanged();
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private ConnectivityManager manager;
    private ConnectivityManager.NetworkCallback network;
    private ConnectivityManager.NetworkCallback vpn;
    private boolean lastValidated;

    private NetworkWatcher(Listener listener) {
        this.listener = listener;
    }

    static NetworkWatcher start(Context context, Listener listener) {
        if (Build.VERSION.SDK_INT < 21) return null;
        NetworkWatcher watcher = new NetworkWatcher(listener);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return watcher;
        watcher.manager = cm;
        watcher.network = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network net) {
                watcher.handler.post(listener::onNetworkChanged);
            }

            @Override
            public void onLost(Network net) {
                watcher.handler.post(listener::onNetworkChanged);
            }

            @Override
            public void onCapabilitiesChanged(Network net, NetworkCapabilities caps) {
                boolean validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                if (validated == watcher.lastValidated) return;
                watcher.lastValidated = validated;
                watcher.handler.post(listener::onNetworkChanged);
            }
        };
        watcher.vpn = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network net) {
                watcher.handler.post(listener::onNetworkChanged);
            }

            @Override
            public void onLost(Network net) {
                watcher.handler.post(listener::onNetworkChanged);
            }
        };
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                cm.registerDefaultNetworkCallback(watcher.network);
            } else {
                cm.registerNetworkCallback(new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(), watcher.network);
            }
            cm.registerNetworkCallback(new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build(), watcher.vpn);
        } catch (RuntimeException e) {
            return watcher;
        }
        return watcher;
    }

    void stop() {
        if (manager == null) return;
        try {
            if (network != null) manager.unregisterNetworkCallback(network);
        } catch (RuntimeException ignored) {
        }
        try {
            if (vpn != null) manager.unregisterNetworkCallback(vpn);
        } catch (RuntimeException ignored) {
        }
    }
}
