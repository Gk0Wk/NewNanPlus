package city.newnan.newnanplus.utility;

/* This is a copy of ItemUtils.class in WolfyUtilities Library   *\
\*  Source: https://github.com/WolfyScript/WolfyUtilities        */

import city.newnan.newnanplus.NewNanPlus;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import city.newnan.api.Reflection;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ItemKit {
    private static final Class<?> nbtTagCompoundClazz = Reflection.getNMS("NBTTagCompound");
    private static final Method asNMSCopyMethod = Reflection.getMethod(Reflection.getOBC("inventory.CraftItemStack"), "asNMSCopy", ItemStack.class);
    private static final Method saveNmsItemStackMethod = Reflection.getMethod(Reflection.getNMS("ItemStack"), "save", nbtTagCompoundClazz);
    public static String toJSON(ItemStack itemStack) {
        Object itemAsJsonObject;
        try {
            Object nmsNbtTagCompoundObj = nbtTagCompoundClazz.getDeclaredConstructor(new Class[0]).newInstance();
            Object nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
            itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);
        } catch (InstantiationException|java.lang.reflect.InvocationTargetException|NoSuchMethodException|IllegalAccessException e) {
            NewNanPlus.getPlugin().messageManager.info("failed to serialize itemstack to nms item");
            NewNanPlus.getPlugin().messageManager.info(e.toString());
            for (StackTraceElement element : e.getStackTrace())
                NewNanPlus.getPlugin().messageManager.info(element.toString());
            return null;
        }
        return itemAsJsonObject.toString();
    }

    private static final Method asBukkitCopyMethod = Reflection.getMethod(Reflection.getOBC("inventory.CraftItemStack"), "asBukkitCopy", Reflection.getNMS("ItemStack"));
    private static final Class<?> mojangParser = Reflection.getNMS("MojangsonParser");
    private static final Method parseMethod = Reflection.getMethod(mojangParser, "parse", String.class);
    private static final Method aMethod = Reflection.getMethod(Reflection.getNMS("ItemStack"), "a", Reflection.getNMS("NBTTagCompound"));
    public static ItemStack fromJSON(String json) {
        try {
            Object nmsNbtTagCompoundObj = parseMethod.invoke(null, json);
            Object nmsItemStackObj = aMethod.invoke(null, nmsNbtTagCompoundObj);
            return (ItemStack)asBukkitCopyMethod.invoke(null, new Object[] { nmsItemStackObj });
        } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String serializeItemStackBase64(ItemStack is) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(outputStream);
            bukkitOutputStream.writeObject(is);
            bukkitOutputStream.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize ItemStack!", e);
        }
    }

    public static ItemStack deserializeItemStackBase64(String data) {
        return deserializeItemStackBase64(Base64.getDecoder().decode(data));
    }

    public static ItemStack deserializeItemStackBase64(byte[] bytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(inputStream);
            Object itemStack = bukkitInputStream.readObject();
            if (itemStack instanceof ItemStack)
                return (ItemStack)itemStack;
            return null;
        } catch (StreamCorruptedException ex) {
            return fromBase64(bytes);
        } catch (IOException|ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String toBase64String(ItemStack itemStack) {
        return Base64.getEncoder().encodeToString(toBase64(itemStack));
    }

    public static byte[] toBase64(ItemStack itemStack) {
        if (itemStack == null)
            return null;
        ByteArrayOutputStream outputStream = null;
        try {
            Class<?> nbtTagCompoundClass = Reflection.getNMS("NBTTagCompound");
            Constructor<?> nbtTagCompoundConstructor = nbtTagCompoundClass.getConstructor();
            Object nbtTagCompound = nbtTagCompoundConstructor.newInstance();
            Object nmsItemStack = Objects.requireNonNull(Reflection.getOBC("inventory.CraftItemStack")).getMethod("asNMSCopy", ItemStack.class).invoke(null, itemStack);
            Reflection.getNMS("ItemStack").getMethod("save", nbtTagCompoundClass).invoke(nmsItemStack, nbtTagCompound);
            outputStream = new ByteArrayOutputStream();
            Reflection.getNMS("NBTCompressedStreamTools").getMethod("a", nbtTagCompoundClass, OutputStream.class).invoke(null, nbtTagCompound, outputStream);
        } catch (SecurityException|NoSuchMethodException|InstantiationException|IllegalAccessException|IllegalArgumentException|java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
        }
        assert outputStream != null;
        return outputStream.toByteArray();
    }

    public static ItemStack fromBase64(String data) {
        return fromBase64(Base64.getDecoder().decode(data));
    }

    public static ItemStack fromBase64(byte[] bytes) {
        if (bytes == null)
            return null;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        Class<?> nbtTagCompoundClass = Reflection.getNMS("NBTTagCompound");
        Class<?> nmsItemStackClass = Reflection.getNMS("ItemStack");
        ItemStack itemStack = null;
        try {
            Object nbtTagCompound = Reflection.getNMS("NBTCompressedStreamTools").getMethod("a", InputStream.class).invoke(null, inputStream);
            Object craftItemStack = nmsItemStackClass.getMethod("a", nbtTagCompoundClass).invoke(nmsItemStackClass, nbtTagCompound);
            itemStack = (ItemStack) Objects.requireNonNull(Reflection.getOBC("inventory.CraftItemStack")).getMethod("asBukkitCopy", nmsItemStackClass).invoke(null, new Object[] { craftItemStack });
        } catch (IllegalAccessException|IllegalArgumentException|java.lang.reflect.InvocationTargetException|NoSuchMethodException|SecurityException e) {
            e.printStackTrace();
        }
        return itemStack;
    }

    /**
     * 通过指定材质url来获取对应的头颅
     * @param url 材质url
     * @return 一个拥有材质的头颅
     * @throws Exception 任何异常
     */
    public static ItemStack getSkull(String url) throws Exception {
        // 创建一个头
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;

        // 创建一个随机的UUID虚拟玩家，并赋予对应的材质
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        // 如果不是http资源链接就添加默认的
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://textures.minecraft.net/texture/" + url;
        }
        profile.getProperties().put("textures", new Property("textures",
                new String(Base64.getEncoder().encode(("{textures:{SKIN:{url:\"" + url + "\"}}}").getBytes()))));

        // 通过反射将材质信息附加
        Field field = skullMeta.getClass().getDeclaredField("profile");
        field.setAccessible(true);
        field.set(skullMeta, profile);
        skull.setItemMeta(skullMeta);

        return skull;
    }

    /**
     * 通过指定玩家来获取对应的头颅
     * @param player 要获取头颅的所属玩家
     * @return 一个拥有材质的头颅
     */
    public static ItemStack getSkull(OfflinePlayer player) {
        // 创建一个头
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;
        // 指定头的主人
        skullMeta.setOwningPlayer(player);
        skull.setItemMeta(skullMeta);
        return skull;
    }
}
