package city.newnan.newnanplus.utility;

import city.newnan.newnanplus.NewNanPlus;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class Reflection {
    private static String versionString;

    private static final Map<String, Class<?>> loadedNMSClasses = new HashMap<>();

    private static final Map<String, Class<?>> loadedOBCClasses = new HashMap<>();

    private static final Map<Class<?>, Map<String, Method>> loadedMethods = new HashMap<>();

    private static final Map<Class<?>, Map<String, Method>> loadedDeclaredMethods = new HashMap<>();

    private static final Map<Class<?>, Map<String, Field>> loadedFields = new HashMap<>();

    private static final Map<Class<?>, Map<String, Field>> loadedDeclaredFields = new HashMap<>();

    private static final Map<Class<?>, Map<Class<?>, Field>> foundFields = new HashMap<>();

    public static String getVersion() {
        return NewNanPlus.getPlugin().getServer().getClass().getPackage().getName().substring(23);
    }

    public static synchronized Class<?> getOBC(String obcClassName) {
        Class<?> clazz;
        if (loadedOBCClasses.containsKey(obcClassName))
            return loadedOBCClasses.get(obcClassName);
        String clazzName = "org.bukkit.craftbukkit." + getVersion() + "." + obcClassName;
        try {
            clazz = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            loadedOBCClasses.put(obcClassName, null);
            return null;
        }
        loadedOBCClasses.put(obcClassName, clazz);
        return clazz;
    }

    public static Class<?> getNMS(String nmsClassName) {
        Class<?> clazz;
        if (loadedNMSClasses.containsKey(nmsClassName))
            return loadedNMSClasses.get(nmsClassName);
        String clazzName = "net.minecraft.server." + getVersion() + "." + nmsClassName;
        try {
            clazz = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return loadedNMSClasses.put(nmsClassName, null);
        }
        loadedNMSClasses.put(nmsClassName, clazz);
        return clazz;
    }

    public static Object getConnection(Player player) {
        Method getHandleMethod = getMethod(player.getClass(), "getHandle");
        if (getHandleMethod != null)
            try {
                Object nmsPlayer = getHandleMethod.invoke(player);
                Field playerConField = getField(nmsPlayer.getClass(), "playerConnection");
                assert playerConField != null;
                return playerConField.get(nmsPlayer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        return null;
    }

    public static Constructor<?> getConstructor(@NotNull Class<?> clazz, Class<?>... params) {
        try {
            return clazz.getConstructor(params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Method getMethod(@NotNull Class<?> clazz, String methodName, Class<?>... params) {
        return getMethod(false, clazz, methodName, params);
    }

    public static Method getMethod(boolean silent, @NotNull Class<?> clazz, String methodName, Class<?>... params) {
        if (!loadedMethods.containsKey(clazz))
            loadedMethods.put(clazz, new HashMap<>());
        Map<String, Method> methods = loadedMethods.get(clazz);
        if (methods.containsKey(methodName))
            return methods.get(methodName);
        try {
            Method method = clazz.getMethod(methodName, params);
            methods.put(methodName, method);
            loadedMethods.put(clazz, methods);
            return method;
        } catch (Exception e) {
            if (!silent)
                e.printStackTrace();
            methods.put(methodName, null);
            loadedMethods.put(clazz, methods);
            return null;
        }
    }

    public static Method getDeclaredMethod(@NotNull Class<?> clazz, String methodName, Class<?>... params) {
        return getDeclaredMethod(false, clazz, methodName, params);
    }

    public static Method getDeclaredMethod(boolean silent, @NotNull Class<?> clazz, String methodName, Class<?>... params) {
        if (!loadedDeclaredMethods.containsKey(clazz))
            loadedDeclaredMethods.put(clazz, new HashMap<>());
        Map<String, Method> methods = loadedDeclaredMethods.get(clazz);
        if (methods.containsKey(methodName))
            return methods.get(methodName);
        try {
            Method method = clazz.getDeclaredMethod(methodName, params);
            methods.put(methodName, method);
            loadedDeclaredMethods.put(clazz, methods);
            return method;
        } catch (Exception e) {
            if (!silent)
                e.printStackTrace();
            methods.put(methodName, null);
            loadedDeclaredMethods.put(clazz, methods);
            return null;
        }
    }

    public static Field getField(@NotNull Class<?> clazz, String fieldName) {
        if (!loadedFields.containsKey(clazz))
            loadedFields.put(clazz, new HashMap<>());
        Map<String, Field> fields = loadedFields.get(clazz);
        if (fields.containsKey(fieldName))
            return fields.get(fieldName);
        try {
            Field field = clazz.getField(fieldName);
            fields.put(fieldName, field);
            loadedFields.put(clazz, fields);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
            fields.put(fieldName, null);
            loadedFields.put(clazz, fields);
            return null;
        }
    }

    public static Field getDeclaredField(@NotNull Class<?> clazz, String fieldName) {
        if (!loadedDeclaredFields.containsKey(clazz))
            loadedDeclaredFields.put(clazz, new HashMap<>());
        Map<String, Field> fields = loadedDeclaredFields.get(clazz);
        if (fields.containsKey(fieldName))
            return fields.get(fieldName);
        try {
            Field field = clazz.getDeclaredField(fieldName);
            fields.put(fieldName, field);
            loadedDeclaredFields.put(clazz, fields);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
            fields.put(fieldName, null);
            loadedDeclaredFields.put(clazz, fields);
            return null;
        }
    }

    public static Field findField(@NotNull Class<?> clazz, Class<?> type) {
        if (!foundFields.containsKey(clazz))
            foundFields.put(clazz, new HashMap<>());
        Map<Class<?>, Field> fields = foundFields.get(clazz);
        if (fields.containsKey(type))
            return fields.get(type);
        try {
            List<Field> allFields = new ArrayList<>();
            allFields.addAll(Arrays.asList(clazz.getFields()));
            allFields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            for (Field f : allFields) {
                if (type.equals(f.getType())) {
                    fields.put(type, f);
                    foundFields.put(clazz, fields);
                    return f;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fields.put(type, null);
            foundFields.put(clazz, fields);
        }
        return null;
    }
}
