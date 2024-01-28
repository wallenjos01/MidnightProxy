package org.wallentines.mdproxy.util;

import java.util.Random;

public class StringUtil {


    private static final Random RANDOM = new Random();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String randomId(int length) {

        StringBuilder out = new StringBuilder(length);
        for(int i = 0 ; i < length ; i++) {
            out.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }

        return out.toString();
    }

}
