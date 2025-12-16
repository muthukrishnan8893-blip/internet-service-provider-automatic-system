package com.isp.util;

import java.util.UUID;

/**
 * Utility class for generating unique IDs.
 */
public class IdGenerator {
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
