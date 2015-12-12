package com.crossbowffs.nekosms.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionUtils {
    private ReflectionUtils() { }

    public static Method getDeclaredMethod(Class<?> cls, String methodName, Class<?>... paramTypes) {
        Method method;
        try {
            method = cls.getDeclaredMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        method.setAccessible(true);
        return method;
    }

    public static Method getMethod(Class<?> cls, String methodName, Class<?>... paramTypes) {
        Method method;
        try {
            method = cls.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        method.setAccessible(true);
        return method;
    }

    public static Object invoke(Method method, Object thisObject, Object... params) {
        try {
            return method.invoke(thisObject, params);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                throw new RuntimeException();
            } else {
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
