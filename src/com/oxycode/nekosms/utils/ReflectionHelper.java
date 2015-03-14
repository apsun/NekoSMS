package com.oxycode.nekosms.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionHelper {
    public static Object invoke(Object self, String methodName, Object... args) {
        return invokeHelper(false, self, methodName, args);
    }

    public static Object invokeStatic(Object cls, String methodName, Object... args) {
        return invokeHelper(true, cls, methodName, args);
    }

    public static String dumpFields(Object self, boolean fullNames, boolean dumpValues) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> cls = self.getClass();; cls = cls.getSuperclass()) {
            sb.append("----------------------------------------\n");
            sb.append("Fields for: ");
            sb.append(fullNames ? cls.getName() : cls.getSimpleName());
            sb.append("\n----------------------------------------\n");
            for (Field field : cls.getDeclaredFields()) {
                if (field.isSynthetic() || field.getName().startsWith("shadow$_")) {
                    continue;
                }

                sb.append(field.getName());
                if (dumpValues) {
                    sb.append(" = ");
                    field.setAccessible(true);
                    Object fieldValue;
                    try {
                        fieldValue = field.get(self);
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                    sb.append(fieldValue);
                }
                sb.append('\n');
            }

            if (cls.getSuperclass() == null) {
                break;
            }
        }
        return sb.toString();
    }

    public static String dumpMethods(Object self, boolean fullNames) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> cls = self.getClass();; cls = cls.getSuperclass()) {
            sb.append("----------------------------------------\n");
            sb.append("Methods for: ");
            sb.append(fullNames ? cls.getName() : cls.getSimpleName());
            sb.append("\n----------------------------------------\n");
            for (Method method : cls.getDeclaredMethods()) {
                sb.append(method.getName());
                sb.append('(');

                Class<?>[] argTypes = method.getParameterTypes();

                if (argTypes.length == 1) {
                    sb.append(argTypes[0].getName());
                } else if (argTypes.length > 1) {
                    for (int i = 0; i < argTypes.length; i++) {
                        Class<?> argType = argTypes[i];
                        sb.append("\n  ");
                        sb.append(argType.getName());
                        if (i < argTypes.length - 1) {
                            sb.append(',');
                        }
                    }
                }

                sb.append(")\n");
            }

            if (cls.getSuperclass() == null) {
                break;
            }
        }
        return sb.toString();
    }

    private static Field getFieldRecursive(Class<?> cls, String fieldName) {
        do {
            try {
                return cls.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Ignore until end
            }
        } while ((cls = cls.getSuperclass()) != null);
        throw new IllegalArgumentException("Could not find field: " + fieldName);
    }

    private static Method getMethodRecursive(Class<?> cls, String methodName, Class<?>... argTypes) {
        do {
            try {
                return cls.getDeclaredMethod(methodName, argTypes);
            } catch (NoSuchMethodException e) {
                // Ignore until end
            }
        } while ((cls = cls.getSuperclass()) != null);
        throw new IllegalArgumentException("Could not find method: " + methodName);
    }

    public static Object getFieldValue(Object self, String fieldName) {
        Class<?> cls = self.getClass();
        Field field = getFieldRecursive(cls, fieldName);
        field.setAccessible(true);
        try {
            return field.get(self);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static Class<?> convertToClass(Object cls) {
        if (cls instanceof String) {
            try {
                return Class.forName((String)cls);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (cls instanceof Class<?>) {
            return (Class<?>)cls;
        } else {
            throw new IllegalArgumentException("Class must be instance of String or Class<?>");
        }
    }

    private static Object invokeHelper(boolean isStatic, Object selfOrCls, String methodName, Object... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide a whole number of arg type-value pairs");
        }

        Object self;
        Class<?> cls;
        if (isStatic) {
            self = null;
            cls = convertToClass(selfOrCls);
        } else {
            self = selfOrCls;
            cls = selfOrCls.getClass();
        }

        Class<?>[] argTypes = new Class<?>[args.length / 2];
        Object[] argValues = new Object[args.length / 2];
        for (int i = 0, j = 0; i < args.length; i += 2, j++) {
            argTypes[j] = convertToClass(args[i]);
            argValues[j] = args[i + 1];
        }

        Method method = getMethodRecursive(cls, methodName, argTypes);
        method.setAccessible(true);
        try {
            return method.invoke(self, argValues);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
