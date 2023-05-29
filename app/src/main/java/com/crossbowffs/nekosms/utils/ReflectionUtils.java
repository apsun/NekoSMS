package com.crossbowffs.nekosms.utils;

import java.lang.reflect.*;

public final class ReflectionUtils {
    private ReflectionUtils() { }

    public static Class<?> getClass(ClassLoader classLoader, String name) {
        try {
            return Class.forName(name, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getDeclaredField(Class<?> cls, String fieldName) {
        Field field;
        try {
            field = cls.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true);
        return field;
    }

    public static Field getField(Class<?> cls, String fieldName) {
        Field field;
        try {
            field = cls.getField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
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

    public static void setFieldValue(Field field, Object object, Object value) {
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method getDeclaredMethod(Class<?> cls, String methodName, Class<?>... paramTypes) {
        Method method;
        try {
            method = cls.getDeclaredMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        method.setAccessible(true);
        return method;
    }

    public static Method getMethod(Class<?> cls, String methodName, Class<?>... paramTypes) {
        Method method;
        try {
            method = cls.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        method.setAccessible(true);
        return method;
    }

    public static Object invoke(Method method, Object thisObject, Object... params) {
        try {
            return method.invoke(thisObject, params);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            } else {
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static String dumpModifiers(int mod) {
        String modString = Modifier.toString(mod);
        if (modString.isEmpty()) {
            return modString;
        } else {
            return modString + " ";
        }
    }

    private static String dumpParameterList(Class<?>[] ps) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        boolean first = true;
        for (Class<?> p : ps) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(p.getCanonicalName());
        }
        sb.append(')');
        return sb.toString();
    }

    private static String dumpConstructor(Constructor<?> c) {
        StringBuilder sb = new StringBuilder();
        sb.append(dumpModifiers(c.getModifiers()));
        sb.append(c.getDeclaringClass().getSimpleName());
        sb.append(dumpParameterList(c.getParameterTypes()));
        sb.append(';');
        return sb.toString();
    }

    private static String dumpMethod(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(dumpModifiers(m.getModifiers()));
        sb.append(m.getReturnType().getName());
        sb.append(' ');
        sb.append(m.getName());
        sb.append(dumpParameterList(m.getParameterTypes()));
        sb.append(';');
        return sb.toString();
    }

    private static String dumpField(Field f) {
        StringBuilder sb = new StringBuilder();
        sb.append(dumpModifiers(f.getModifiers()));
        sb.append(f.getType().getName());
        sb.append(' ');
        sb.append(f.getName());
        sb.append(';');
        return sb.toString();
    }

    private static String dumpSuperclass(Class<?> cls) {
        StringBuilder sb = new StringBuilder();
        if (cls != null) {
            sb.append(" extends ");
            sb.append(cls.getName());
        }
        return sb.toString();
    }

    private static String dumpInterfaces(Class<?>[] ifaces) {
        StringBuilder sb = new StringBuilder();
        if (ifaces.length > 0) {
            sb.append(" implements ");
            boolean first = true;
            for (Class<?> iface : ifaces) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(iface.getName());
            }
        }
        return sb.toString();
    }

    public static String dumpClass(Class<?> cls) {
        StringBuilder sb = new StringBuilder();
        sb.append(dumpModifiers(cls.getModifiers()));
        sb.append("class ");
        sb.append(cls.getName());
        sb.append(dumpSuperclass(cls.getSuperclass()));
        sb.append(dumpInterfaces(cls.getInterfaces()));
        sb.append(" {\n");
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            if (!c.isSynthetic()) {
                sb.append("    ");
                sb.append(dumpConstructor(c));
                sb.append('\n');
            }
        }
        sb.append("\n");
        for (Method m : cls.getDeclaredMethods()) {
            if (!m.isSynthetic()) {
                sb.append("    ");
                sb.append(dumpMethod(m));
                sb.append('\n');
            }
        }
        sb.append("\n");
        for (Field f : cls.getDeclaredFields()) {
            if (!f.isSynthetic()) {
                sb.append("    ");
                sb.append(dumpField(f));
                sb.append('\n');
            }
        }
        sb.append('}');
        return sb.toString();
    }
}