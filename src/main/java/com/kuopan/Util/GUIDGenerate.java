package com.kuopan.Util;

import java.util.UUID;

public class GUIDGenerate {
    public static String GUIDNormGenerate() {
        String r = UUID.randomUUID().toString();
        return r;
    }

}
