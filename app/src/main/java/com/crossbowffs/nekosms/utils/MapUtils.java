package com.crossbowffs.nekosms.utils;

import android.content.ContentValues;

import java.util.HashMap;

public final class MapUtils {
    private MapUtils() { }

    public static int capacityForSize(int size) {
        // The current implementation of HashMap resizes
        // when size > 3/4 of capacity. Therefore we should
        // make the initial capacity 4/3 times the expected
        // element count, rounded up to the nearest integer.
        // Note that this is an implementation detail and
        // can change at any time.
        return (size * 4 + 2) / 3;
    }

    public static ContentValues contentValuesForSize(int size) {
        return new ContentValues(capacityForSize(size));
    }

    public static <K, V> HashMap<K, V> hashMapForSize(int size) {
        return new HashMap<>(capacityForSize(size));
    }
}
