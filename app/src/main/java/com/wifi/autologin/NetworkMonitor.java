package com.wifi.autologin;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

public class NetworkMonitor {
    private Context context;
    private NetworkStateListener listener;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    
    public interface NetworkStateListener {
        void onNetworkChanged(boolean isConnected);
        void onWifiConnected(String ssid);
    }

    public NetworkMonitor(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void setNetworkStateListener(NetworkStateListener listener) {
        this.listener = listener;
    }

    public void startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startNetworkCallback();
        } else {
            startLegacyMonitoring();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startNetworkCallback() {
        connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                if (listener != null) {
                    listener.onNetworkChanged(true);
                    checkWifiConnection();
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                if (listener != null) {
                    listener.onNetworkChanged(false);
                }
            }
        });
    }

    private void startLegacyMonitoring() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (listener != null) {
                    listener.onNetworkChanged(networkInfo != null && networkInfo.isConnected());
                    if (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        checkWifiConnection();
                    }
                }
            }
        }, filter);
    }

    private void checkWifiConnection() {
        String ssid = getCurrentSSID();
        if (ssid != null && !ssid.equals("<unknown ssid>")) {
            if (listener != null && (ssid.contains("VIT-AP") || ssid.contains("vitap"))) {
                listener.onWifiConnected(ssid);
            }
        }
    }

    private String getCurrentSSID() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String ssid = wifiInfo.getSSID();
                    // Handle the case where SSID might be null or unknown
                    if (ssid != null && !ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                        // Remove quotes if present (some Android versions return SSID with quotes)
                        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                            ssid = ssid.substring(1, ssid.length() - 1);
                        }
                        return ssid;
                    }
                }
            }
        }
        return null;
    }

    public boolean isOnCollegeWifi() {
        String ssid = getCurrentSSID();
        return ssid != null && (ssid.contains("VIT-AP") || ssid.contains("vitap"));
    }
}
