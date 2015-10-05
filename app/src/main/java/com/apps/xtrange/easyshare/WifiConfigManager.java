/*
 * Copyright (C) 2011 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apps.xtrange.easyshare;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.util.regex.Pattern;

/**
 * @author Vikram Aggarwal
 * @author Sean Owen
 */
public final class WifiConfigManager extends AsyncTask<String,Object,Object> {

    private static final String TAG = WifiConfigManager.class.getSimpleName();

    private static final Pattern HEX_DIGITS = Pattern.compile("[0-9A-Fa-f]+");

    private OnNetworkUpdateListener mNetworkUpdatedListener;

    private final WifiManager mWifiManager;

    public WifiConfigManager(WifiManager wifiManager, OnNetworkUpdateListener listener) {
        this.mNetworkUpdatedListener = listener;
        this.mWifiManager = wifiManager;
    }

    @Override
    protected void onPostExecute(Object o) {
        if (mNetworkUpdatedListener != null) {
            mNetworkUpdatedListener.onNetworkUpdated();
        }
    }

    @Override
    protected Object doInBackground(String... args) {
        // Start WiFi, otherwise nothing will work
        if (!mWifiManager.isWifiEnabled()) {
            Log.i(TAG, "Enabling wi-fi...");
            if (mWifiManager.setWifiEnabled(true)) {
                Log.i(TAG, "Wi-fi enabled");
            } else {
                Log.w(TAG, "Wi-fi could not be enabled!");
                return null;
            }
            // This happens very quickly, but need to wait for it to enable. A little busy wait?
            int count = 0;
            while (!mWifiManager.isWifiEnabled()) {
                if (count >= 10) {
                    Log.i(TAG, "Took too long to enable wi-fi, quitting");
                    return null;
                }
                Log.i(TAG, "Still waiting for wi-fi to enable...");
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ie) {
                    // continue
                }
                count++;
            }
        }

        String ssid = args[0];
        String password = args[1];
        String networkTypeString = args[2];

        if (networkTypeString.equals(Constants.ENCRYPTION_OPEN)) {
            changeNetworkUnEncrypted(mWifiManager, ssid);
        } else {
            if (password != null && !password.isEmpty()) {
                if (networkTypeString.equals(Constants.ENCRYPTION_WEP)) {
                    changeNetworkWEP(mWifiManager, ssid, password);
                } else if (networkTypeString.equals(Constants.ENCRYPTION_WPA)) {
                    changeNetworkWPA(mWifiManager, ssid, password);
                }
            }
        }
        return null;
    }

    /**
     * Update the network: either create a new network or modify an existing network
     * @param config the new network configuration
     */
    private static void updateNetwork(WifiManager wifiManager, WifiConfiguration config) {
        Integer foundNetworkID = findNetworkInExistingConfig(wifiManager, config.SSID);
        if (foundNetworkID != null) {
            Log.i(TAG, "Removing old configuration for network " + config.SSID);
            wifiManager.removeNetwork(foundNetworkID);
            wifiManager.saveConfiguration();
        }
        int networkId = wifiManager.addNetwork(config);
        if (networkId >= 0) {
            // Try to disable the current network and start a new one.
            if (wifiManager.enableNetwork(networkId, true)) {
                Log.i(TAG, "Associating to network " + config.SSID);
                wifiManager.saveConfiguration();
            } else {
                Log.w(TAG, "Failed to enable network " + config.SSID);
            }
        } else {
            Log.w(TAG, "Unable to add network " + config.SSID);
        }
    }

    private static WifiConfiguration changeNetworkCommon(String ssid) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        // Android API insists that an ascii SSID must be quoted to be correctly handled.
        config.SSID = quoteNonHex(ssid);
        //TODO:Support for hidden networks
        //config.hiddenSSID = wifiResult.isHidden();
        return config;
    }

    // Adding a WEP network
    private static void changeNetworkWEP(WifiManager wifiManager, String ssid, String password) {
        WifiConfiguration config = changeNetworkCommon(ssid);
        config.wepKeys[0] = quoteNonHex(password, 10, 26, 58);
        config.wepTxKeyIndex = 0;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        updateNetwork(wifiManager, config);
    }

    // Adding a WPA or WPA2 network
    private static void changeNetworkWPA(WifiManager wifiManager, String ssid, String password) {
        WifiConfiguration config = changeNetworkCommon(ssid);
        // Hex passwords that are 64 bits long are not to be quoted.
        config.preSharedKey = quoteNonHex(password, 64);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        updateNetwork(wifiManager, config);
    }

    // Adding an open, unsecured network
    private static void changeNetworkUnEncrypted(WifiManager wifiManager, String ssid) {
        WifiConfiguration config = changeNetworkCommon(ssid);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        updateNetwork(wifiManager, config);
    }

    private static Integer findNetworkInExistingConfig(WifiManager wifiManager, String ssid) {
        Iterable<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                String existingSSID = existingConfig.SSID;
                if (existingSSID != null && existingSSID.equals(ssid)) {
                    return existingConfig.networkId;
                }
            }
        }
        return null;
    }

    private static String quoteNonHex(String value, int... allowedLengths) {
        return isHexOfLength(value, allowedLengths) ? value : convertToQuotedString(value);
    }

    /**
     * Encloses the incoming string inside double quotes, if it isn't already quoted.
     * @param s the input string
     * @return a quoted string, of the form "input".  If the input string is null, it returns null
     * as well.
     */
    private static String convertToQuotedString(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        // If already quoted, return as-is
        if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s;
        }
        return '\"' + s + '\"';
    }

    /**
     * @param value input to check
     * @param allowedLengths allowed lengths, if any
     * @return true if value is a non-null, non-empty string of hex digits, and if allowed lengths are given, has
     *  an allowed length
     */
    private static boolean isHexOfLength(CharSequence value, int... allowedLengths) {
        if (value == null || !HEX_DIGITS.matcher(value).matches()) {
            return false;
        }
        if (allowedLengths.length == 0) {
            return true;
        }
        for (int length : allowedLengths) {
            if (value.length() == length) {
                return true;
            }
        }
        return false;
    }

    public interface OnNetworkUpdateListener {
        public void onNetworkUpdated();
    }
}