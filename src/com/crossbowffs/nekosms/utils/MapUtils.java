package com.crossbowffs.nekosms.utils;

import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

public final class MapUtils {
    private MapUtils() { }

    public static <T, U> Map<T, U> createFromArrays(T[] keys, U[] values) {
        int count = keys.length;
        if (count != values.length) {
            throw new IllegalArgumentException("keys and values must have the same length");
        }

        HashMap<T, U> map = new HashMap<T, U>(count);
        for (int i = 0; i < count; ++i) {
            map.put(keys[i], values[i]);
        }

        return map;
    }

    public static <T>SparseArray<T> createFromArrays(int[] keys, T[] values) {
        int count = keys.length;
        if (count != values.length) {
            throw new IllegalArgumentException("keys and values must have the same length");
        }

        SparseArray<T> map = new SparseArray<T>(count);
        for (int i = 0; i < count; ++i) {
            map.put(keys[i], values[i]);
        }

        return map;
    }
}
