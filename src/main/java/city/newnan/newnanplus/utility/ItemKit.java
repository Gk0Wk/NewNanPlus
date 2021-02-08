package city.newnan.newnanplus.utility;

/* This is a copy of ItemUtils.class in WolfyUtilities Library   *\
\*  Source: https://github.com/WolfyScript/WolfyUtilities        */

import city.newnan.newnanplus.NewNanPlus;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ItemKit {
    private static final Class<?> craftItemStackClazz = Reflection.getOBC("inventory.CraftItemStack");

    private static final Class<?> nmsItemStackClazz = Reflection.getNMS("ItemStack");

    private static final Class<?> nbtTagCompoundClazz = Reflection.getNMS("NBTTagCompound");

    public static String convertItemStackToJson(ItemStack itemStack) {
        Object itemAsJsonObject;
        assert craftItemStackClazz != null;
        Method asNMSCopyMethod = Reflection.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);
        Class<?> nbtTagCompoundClazz = Reflection.getNMS("NBTTagCompound");
        Method saveNmsItemStackMethod = Reflection.getMethod(nmsItemStackClazz, "save", nbtTagCompoundClazz);
        try {
            Object nmsNbtTagCompoundObj = nbtTagCompoundClazz.getDeclaredConstructor(new Class[0]).newInstance();
            Object nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
            itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);
        } catch (InstantiationException|java.lang.reflect.InvocationTargetException|NoSuchMethodException|IllegalAccessException e) {
            NewNanPlus.getPlugin().messageManager.printINFO("failed to serialize itemstack to nms item");
            NewNanPlus.getPlugin().messageManager.printINFO(e.toString());
            for (StackTraceElement element : e.getStackTrace())
                NewNanPlus.getPlugin().messageManager.printINFO(element.toString());
            return null;
        }
        return itemAsJsonObject.toString();
    }

    public static ItemStack convertJsontoItemStack(String json) {
        Class<?> mojangParser = Reflection.getNMS("MojangsonParser");
        Method parseMethod = Reflection.getMethod(mojangParser, "parse", String.class);
        Method aMethod = Reflection.getMethod(nmsItemStackClazz, "a", nbtTagCompoundClazz);
        assert craftItemStackClazz != null;
        Method asBukkitCopyMethod = Reflection.getMethod(craftItemStackClazz, "asBukkitCopy", nmsItemStackClazz);
        try {
            Object nmsNbtTagCompoundObj = parseMethod.invoke(null, json);
            Object nmsItemStackObj = aMethod.invoke(null, nmsNbtTagCompoundObj);
            return (ItemStack)asBukkitCopyMethod.invoke(null, new Object[] { nmsItemStackObj });
        } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Deprecated
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

    @Deprecated
    public static ItemStack deserializeItemStackBase64(String data) {
        return deserializeItemStackBase64(Base64.getDecoder().decode(data));
    }

    @Deprecated
    public static ItemStack deserializeItemStackBase64(byte[] bytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(inputStream);
            Object itemStack = bukkitInputStream.readObject();
            if (itemStack instanceof ItemStack)
                return (ItemStack)itemStack;
            return null;
        } catch (StreamCorruptedException ex) {
            return deserializeNMSItemStack(bytes);
        } catch (IOException|ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String serializeNMSItemStack(ItemStack itemStack) {
        if (itemStack == null)
            return "null";
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
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static ItemStack deserializeNMSItemStack(String data) {
        return deserializeNMSItemStack(Base64.getDecoder().decode(data));
    }

    public static ItemStack deserializeNMSItemStack(byte[] bytes) {
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

    public static ItemStack[] createItem(ItemStack itemStack, String displayName, String[] helpLore, String... normalLore) {
        ItemStack[] itemStacks = new ItemStack[2];
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = new ArrayList<>();
        if (itemMeta != null) {
            if (displayName != null && !displayName.isEmpty())
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            if (normalLore != null && normalLore.length > 0) {
                lore = Arrays.stream(normalLore).map(row -> row.equalsIgnoreCase("<empty>") ? "" :
                        ChatColor.translateAlternateColorCodes('&', row)).collect(Collectors.toList());
                itemMeta.setLore(lore);
            }
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            itemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            itemStack.setItemMeta(itemMeta);
        }
        itemStacks[0] = itemStack;
        ItemStack helpItem = new ItemStack(itemStack);
        ItemMeta helpMeta = helpItem.getItemMeta();
        if (helpMeta != null) {
            if (helpLore != null && helpLore.length > 0)
                lore.addAll(Arrays.stream(helpLore).map(row -> row.equalsIgnoreCase("<empty>") ? "" :
                                ChatColor.translateAlternateColorCodes('&', row)).collect(Collectors.toList()));
            helpMeta.setLore(lore);
            helpItem.setItemMeta(helpMeta);
        }
        itemStacks[1] = helpItem;
        return itemStacks;
    }
}
