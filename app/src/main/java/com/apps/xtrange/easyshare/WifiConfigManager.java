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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    private boolean mIsCreatingHotspot;

    public WifiConfigManager(WifiManager wifiManager, boolean creatingHotspot, OnNetworkUpdateListener listener) {
        this.mNetworkUpdatedListener = listener;
        this.mWifiManager = wifiManager;
        this.mIsCreatingHotspot = creatingHotspot;
    }

    public WifiConfigManager(WifiManager wifiManager, OnNetworkUpdateListener listener) {
        this(wifiManager, false, listener);
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
        if (isApEnabled(mWifiManager)) {
            Method setWifiApMethod = null;
            try {
                setWifiApMethod = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);

                Method isHotspotEnabled = mWifiManager.getClass().getMethod("isWifiApEnabled");
                setWifiApMethod.invoke(mWifiManager, null, false);
                int count = 0;
                while ((Boolean)isHotspotEnabled.invoke(mWifiManager)) {
                    if (count >= 10) {
                        Log.i(TAG, "Took too long to disable hotspot, quitting");
                        return null;
                    }
                    Log.i(TAG, "Still waiting for hotspot to disable...");
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ie) {
                        // continue
                    }
                    count++;
                }

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }

        if (!mWifiManager.isWifiEnabled() && !mIsCreatingHotspot) {
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
        } else if (mWifiManager.isWifiEnabled() && mIsCreatingHotspot) {
            mWifiManager.setWifiEnabled(false);
        }

        String ssid = args[0];
        String password = args[1];
        String networkTypeString = args[2];

        WifiConfiguration configuration = null;
        if (networkTypeString.equals(Constants.ENCRYPTION_OPEN)) {
            configuration = createUnencryptedConfiguration(ssid, mIsCreatingHotspot);
        } else {
            if (password != null && !password.isEmpty()) {
                if (networkTypeString.equals(Constants.ENCRYPTION_WEP)) {
                    configuration = createWepConfiguration(ssid, password, mIsCreatingHotspot);
                } else if (networkTypeString.equals(Constants.ENCRYPTION_WPA) && !mIsCreatingHotspot) {
                    configuration = createWpaConfiguration(ssid, password, mIsCreatingHotspot);
                } else if (networkTypeString.equals(Constants.ENCRYPTION_WPA) && mIsCreatingHotspot) {
                    configuration = createWpa2Configuration(ssid, password, mIsCreatingHotspot);
                }
            }
        }

        if (configuration != null) {

            if (!mIsCreatingHotspot)
                updateNetwork(mWifiManager, configuration);
            else
                createHotspot(mWifiManager, configuration);
        }
        return null;
    }

    /**
     * Creates a Wifi Hotspot for @param configuration
     * @param wifiManager
     * @param configuration
     */
    private void createHotspot(WifiManager wifiManager, WifiConfiguration configuration) {
        try{
            Log.d(TAG, "About to create the HotSpot");
            Method setWifiApMethod = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            boolean apstatus=(Boolean) setWifiApMethod.invoke(wifiManager, configuration, true);

            /*Method isWifiApEnabledmethod = wifiManager.getClass().getMethod("isWifiApEnabled");
            while(!(Boolean)isWifiApEnabledmethod.invoke(wifiManager)){};
            Method getWifiApStateMethod = wifiManager.getClass().getMethod("getWifiApState");
            int apstate=(Integer)getWifiApStateMethod.invoke(wifiManager);
            Method getWifiApConfigurationMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");*/
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private static WifiConfiguration changeNetworkCommon(String ssid, boolean isHotspot) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        // Android API insists that an ascii SSID must be quoted to be correctly handled.
        config.SSID = isHotspot ? ssid : quoteNonHex(ssid); //If it's hotspot we won't want the extra double quotes in our SSID
        //TODO:Support for hidden networks
        //config.hiddenSSID = wifiResult.isHidden();
        return config;
    }

    // Adding a WEP network
    private static WifiConfiguration createWepConfiguration(String ssid, String password, boolean isHotspot) {
        WifiConfiguration config = changeNetworkCommon(ssid, isHotspot);
        config.wepKeys[0] = quoteNonHex(password, 10, 26, 58);
        config.wepTxKeyIndex = 0;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        return config;
    }

    private static WifiConfiguration createWpa2Configuration(String ssid, String password, boolean isHotspot) {
        WifiConfiguration config = changeNetworkCommon(ssid, isHotspot);
        // Hex passwords that are 64 bits long are not to be quoted.

        config.preSharedKey = quoteNonHex(password, 64);

        //config.preSharedKey = "\"" + password + "\"";
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);

        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        return config;
    }

    // Adding a WPA or WPA2 network
    private static WifiConfiguration createWpaConfiguration(String ssid, String password, boolean isHotspot) {
        WifiConfiguration config = changeNetworkCommon(ssid, isHotspot);
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
        return config;
    }

    // Adding an open, unsecured network
    private static WifiConfiguration createUnencryptedConfiguration(String ssid, boolean isHotspot) {
        WifiConfiguration config = changeNetworkCommon(ssid, isHotspot);

        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        return config;
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

    /**
     * Returns true if is able to get the methods and this device's Hotspot feature is enabled or false otherwise
     * @param wifiManager
     * @return
     */
    public static boolean isApEnabled(WifiManager wifiManager) {
        try {
            Method isWifiApEnabledmethod = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (Boolean)isWifiApEnabledmethod.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    public interface OnNetworkUpdateListener {
        public void onNetworkUpdated();
    }
}