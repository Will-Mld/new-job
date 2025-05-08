package com.ecwid.test.deep_copy;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * You might need to add jvm parameters to be able to use {@link Field#setAccessible(boolean)}
 * like below
 * --add-opens=java.base/java.lang=ALL-UNNAMED
 * --add-opens=java.base/java.util=ALL-UNNAMED
 * --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
 *
 */
public class CopyUtils {

    /**
     * Deep copy object regardless of fields visibility.
     * @param obj object to copy
     * @return deep copy of the object
     * @param <T> object type
     */
    public static <T> T deepCopy(T obj)
    {
        return deepCopyInternal(obj, new IdentityHashMap<>());
    }

    /**
     * Deep copy object without recursion (recursion will not work on LinkedList for example).
     * @param obj object to copy
     * @param copies map of original object (by reference) to copies to avoid cycles
     * @return deep copy
     * @param <T> type of the object
     */
    public static <T> T deepCopyInternal(T obj, Map<Object, Object> copies)
    {
        Stack<CopyTask> tasks = new Stack<>();
        tasks.push(new CopyTask(obj, null));
        Map<Class<?>, Integer> copyCount = new HashMap<>();
        while (!tasks.isEmpty())
        {
            CopyTask t = tasks.pop();
            int count = copyCount.computeIfAbsent(t.original.getClass(), x -> 0); copyCount.put(t.original.getClass(), ++count);
            Object copy = deepCopyInternal0(t.original, copies, tasks);
            t.propagate(copy);
        }
        System.out.println(copyCount);
        @SuppressWarnings("unchecked") T r = (T) copies.get(obj);
        return r;
    }

    /**
     * Attempts to deep copy object. See {@link #construct(Class, boolean, boolean, Object)}}
     * to understand how deep copies of fields are instantiated. Throws {@link RuntimeException} in case failure.
     *
     * @param obj Object to copy
     * @param copies map between original values and copied. Used to avoid circular dependencies.
     * @return deep copy of the object
     * @param <T> type of the object
     */
    private static <T> T deepCopyInternal0(T obj, Map<Object, Object> copies, Stack<CopyTask> tasks)
    {
        Objects.requireNonNull(obj);

        @SuppressWarnings("unchecked") Class<T> clazz = (Class<T>) obj.getClass();

        if (obj instanceof Cloneable)
        {
            System.err.println("Class " + clazz + " implements Cloneable. You may want to use clone() method.");
        }

        T copy = construct(clazz, true, true, obj);

        Class<?> cl = clazz;
        while (cl != null) {

            Field[] fields = cl.getDeclaredFields();
            for (Field field : fields) {
                try {
                    int modifiers = field.getModifiers();
                    if (!Modifier.isStatic(modifiers)) {
                        field.setAccessible(true);
                        Class<?> type = field.getType();
                        if (type.isPrimitive() || type == String.class) {
                            field.set(copy, field.get(obj));
                        } else if (!type.isArray()) {
                            Object fieldValue = field.get(obj);
                            setCachedOrCopy(fieldValue, x -> {
                                try {
                                    field.set(copy, x);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }, copies, tasks);
                        } else {
                            Class<?> componentType = type.componentType();
                            if (!componentType.isPrimitive()) {
                                Object[] fieldValue = (Object[]) field.get(obj);
                                if (fieldValue != null) {
                                    int length = fieldValue.length;
                                    Object[] arrayValueCopy = (Object[]) Array.newInstance(componentType, length);
                                    for (int k = 0; k < length; k++) {
                                        Object arrayElement = fieldValue[k];
                                        final int k_ = k;
                                        setCachedOrCopy(arrayElement, x -> arrayValueCopy[k_] = x, copies, tasks);
                                    }
                                    field.set(copy, arrayValueCopy);
                                }
                                else
                                {
                                    field.set(copy, null);
                                }
                            } else {
                                Object primitiveArrayCopy = copyPrimitiveArray(field.get(obj), componentType);
                                field.set(copy, primitiveArrayCopy);
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            cl = cl.getSuperclass();
        }

        copies.put(obj, copy);
        return copy;
    }

    /**
     * Used to avoid circular dependencies
     *
     * @param fieldValue value to deep-copy
     * @param setter     set calculated deep copy
     * @param copies     mapping of original values to copied
     * @param tasks      task stack to avoid deep recursion
     */
    private static void setCachedOrCopy(Object fieldValue, Consumer<Object> setter, Map<Object, Object> copies, Stack<CopyTask> tasks) throws IllegalAccessException {

        Object cached = null;
        if (fieldValue != null) {
            cached = copies.get(fieldValue);
            if (cached == null) {
                CopyTask copyTask = new CopyTask(fieldValue, setter);
                tasks.push(copyTask);
                return;
            }
        }
        setter.accept(cached);
    }

    /**
     * Method tries to find default constructor and instantiate Class.
     * If constructor not found or no public constructor, will try non-public default constructor based on {@code allowNonPublicConstructor}.
     * If still not found - use {@link Cloneable}, if presented.
     * If previous didn't work, pick up first constructor with the shortest list of parameters and instantiate with default values. See {@link #getDefaultConstructorArgument}
     *
     * Note, it may fail, if constructor does validations or anything other than assignment operations on arguments.
     *
     * @param clazz Class to instantiate
     * @param allowNonPublicConstructor if we can use non-public constructors
     * @param useNonDefaultConstructor if we can use non-default constructors
     * @param obj instance of Object we are trying to construct. Will be used to clone, if no default constructor found.
     * @return instance of Class. No guaranties of how class instantiated.
     * @param <T> type of the Class
     */
    private static <T> T construct(Class<T> clazz, boolean allowNonPublicConstructor, boolean useNonDefaultConstructor, T obj) {

        Constructor<T> defaultConstructor = getDefaultConstructor(clazz, allowNonPublicConstructor, useNonDefaultConstructor);
        if (defaultConstructor != null) {
            try {
                return defaultConstructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        // Use Cloneable if available
        if (clazz.isInstance(Cloneable.class)) {
            try {
                @SuppressWarnings("unchecked") T r = (T) clazz.getMethod("clone").invoke(obj);
                return r;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        // Use any constructor with the smallest signature 
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> minConstructor = null;
        int minArguments = Integer.MAX_VALUE;
        for (Constructor<?> c : constructors)
        {
            if (Modifier.isPublic(c.getModifiers()) || allowNonPublicConstructor)
            {
                int parameterCount = c.getParameterCount();
                if (parameterCount < minArguments) {
                    minArguments = parameterCount;
                    minConstructor = c;
                }
            }
        }

        if (minConstructor == null)
            throw new IllegalArgumentException("There are no constructors for " + clazz);

        minConstructor.setAccessible(true);
        try {
            Object[] parameters = new Object[minArguments];
            Class<?>[] parameterTypes = minConstructor.getParameterTypes();
            int pc = minConstructor.getParameterCount();
            for (int y = 0; y < pc; y++)
            {
                Class<?> parameterType = parameterTypes[y];
                parameters[y] = getDefaultConstructorArgument(parameterType);
            }
            @SuppressWarnings("unchecked") T r = (T) minConstructor.newInstance(parameters);
            return r;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> Constructor<T> getDefaultConstructor(Class<T> clazz, boolean allowNonPublicConstructor, boolean useNonDefaultConstructor) {
        Constructor<T> defaultConstructor = null;
        try {
            defaultConstructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            if (!useNonDefaultConstructor) {
                throw new IllegalArgumentException("There is no default constructor for " + clazz);
            }
        }
        if (defaultConstructor != null) {
            int modifiers = defaultConstructor.getModifiers();
            if (!Modifier.isPublic(modifiers) && !allowNonPublicConstructor) {
                if (!useNonDefaultConstructor) {
                    throw new IllegalArgumentException("There is no public default constructor for " + clazz);
                }
            }
        }
        return defaultConstructor;
    }

    private static Object copyPrimitiveArray(Object array, Class<?> componentType) {

        if (componentType == int.class)
        {
            int[] a = (int[]) array;
            return Arrays.copyOf(a, a.length);
        } else if (componentType == short.class)
        {
            short[] a = (short[]) array;
            return Arrays.copyOf(a, a.length);
        } else if (componentType == byte.class)
        {
            byte[] a = (byte[]) array;
            return Arrays.copyOf(a, a.length);
        } else if (componentType == long.class)
        {
            long[] a = (long[]) array;
            return Arrays.copyOf(a, a.length);
        } else if (componentType == double.class)
        {
            double[] a = (double[]) array;
            return Arrays.copyOf(a, a.length);
        } else if (componentType == float.class)
        {
            float[] a = (float[]) array;
            return Arrays.copyOf(a, a.length);
        } else if (componentType == boolean.class)
        {
            boolean[] a = (boolean[]) array;
            return Arrays.copyOf(a, a.length);
        }
        else if (componentType == char.class)
        {
            char[] a = (char[]) array;
            return Arrays.copyOf(a, a.length);
        }
        throw new IllegalArgumentException("Unexpected type " + array);
    }

    /**
     * To be used in non-default constructor
     * @param clazz parameter of non-default constructor
     * @return object of type clazz
     */
    private static Object getDefaultConstructorArgument(Class<?> clazz) {
        if (clazz == byte.class) {
            return (byte) 0;
        } else if (clazz == short.class)
        {
            return (short) 0;
        } else if (clazz == int.class)
        {
            return 0;
        } else if (clazz == long.class)
        {
            return 0L;
        } else if (clazz == double.class)
        {
            return (double) 0;
        } else if (clazz == float.class)
        {
            return (float) 0;
        } else if (clazz == boolean.class)
        {
            return Boolean.FALSE;
        }
        else if (clazz == char.class)
        {
            return (char) 0;
        }
        if (clazz.isArray())
        {
            Class<?> componentType = clazz.componentType();
            if (!componentType.isPrimitive())
            {
                return new Object[0];
            }
            else
            {
                if (componentType == byte.class)
                {
                    return new byte[0];
                } else if (componentType == short.class)
                {
                    return new short[0];
                } else if (componentType == int.class)
                {
                        return new int[0];
                } else if (componentType == long.class)
                {
                    return new long[0];
                } else if (componentType == double.class)
                {
                    return new double[0];
                } else if (componentType == float.class)
                {
                    return new float[0];
                } else if (componentType == boolean.class)
                {
                    return new boolean[0];
                } else if (componentType == char.class)
                {
                    return new char[0];
                }
            }
        }
        // just Object
        return null;
    }

    private record CopyTask(Object original, Consumer<Object> copyObserver) {

        public void propagate(Object copy) {
                if (copyObserver != null)
                    copyObserver.accept(copy);
        }
    }

}
