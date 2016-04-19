package com.crossbowffs.nekosms.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionUtils {
    private ReflectionUtils() { }

    public static Field getDeclaredField(Class<?> cls, String fieldName) {
        Field field;
        try {
            field = cls.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
        field.setAccessible(true);
        return field;
    }

    public static Object getFieldValue(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getInstanceFieldValue(Object object, String fieldName) {
        Field field = getDeclaredField(object.getClass(), fieldName);
        return getFieldValue(field, object);
    }

    public static Object getStaticFieldValue(Class<?> cls, String fieldName) {
        Field field = getDeclaredField(cls, fieldName);
        return getFieldValue(field, null);
    }

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
