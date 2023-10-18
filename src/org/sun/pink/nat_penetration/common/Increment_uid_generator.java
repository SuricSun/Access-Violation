package org.sun.pink.nat_penetration.common;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Increment_uid_generator {

    private static long increment_uid = Long.MAX_VALUE;

    public static long Retrieve() {

        synchronized (Increment_uid_generator.class) {
            return ++increment_uid;
        }
    }
}
