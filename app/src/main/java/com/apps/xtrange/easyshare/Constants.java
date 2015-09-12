package com.apps.xtrange.easyshare;

import java.util.regex.Pattern;

/**
 * Created by Oscar on 9/11/2015.
 */
public class Constants {
    public static final int MAGIC_NUMBER = 0x1337;

    public static final Pattern CONTENTS_PATTERN = Pattern.compile("([0-9]{1,3}\\.){3}[0-9]{1,3}:[0-9]+:" +
            String.valueOf(MAGIC_NUMBER) + ":" + "[a-zA-Z]+");
}
