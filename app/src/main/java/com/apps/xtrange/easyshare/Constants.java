package com.apps.xtrange.easyshare;

import java.util.regex.Pattern;

/**
 * Created by Oscar on 9/11/2015.
 */
public class Constants {
    public static final int MAGIC_NUMBER = 0x1337;

    public static final Pattern SHARE_FILES_PATTERN = Pattern.compile("([0-9]{1,3}\\.){3}[0-9]{1,3}:[0-9]+:" +
            String.valueOf(MAGIC_NUMBER) + ":" + "[a-zA-Z]+");

    public static final Pattern SHARE_HOTSPOT_PATTERN = Pattern.compile("([^:]{1,32}):([a-z]+-[a-z]+):([^:]{0,32}):" +
            String.valueOf(MAGIC_NUMBER));

    public static final String BROADCAST_BASE = "com.apps.xtrange.easyshare.";
    public static final String BROADCAST_CLIENT_CONNECTED = BROADCAST_BASE + "CLIENT_CONNECTED";
    public static final String BROADCAST_SERVICE_STARTED = BROADCAST_BASE + "SERVICE_STARTED";
    public static final String BROADCAST_SERVICE_FINISHED = BROADCAST_BASE + "SERVICE_FINISHED";

    public static final String ENCRYPTION_OPEN = "encryption-open";
    public static final String ENCRYPTION_WEP = "encryption-wep";
    public static final String ENCRYPTION_WPA = "encryption-wpa";

    public static final String EXTRA_IP_ADDRESS = "extra-ip";
    public static final String EXTRA_FILENAME = "extra-filename";
    public static final int RC_HANDLE_GMS = 9001;
}
