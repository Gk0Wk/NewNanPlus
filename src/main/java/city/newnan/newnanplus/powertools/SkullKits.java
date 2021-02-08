package city.newnan.newnanplus.powertools;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.exception.CommandExceptions;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.wolfyscript.utilities.util.inventory.PlayerHeadUtils;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;

public class SkullKits {
    private static final Base64.Encoder encoder = Base64.getEncoder();

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
        profile.getProperties().put("textures", new Property("textures",
                new String(encoder.encode(("{textures:{SKIN:{url:\"" + url + "\"}}}").getBytes()))));

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

    /**
     * /nnp skull指令
     * @param sender 指令发送者
     * @param args 指令参数
     * @throws Exception 任何异常
     */
    public static void skullCommand(CommandSender sender, String[] args) throws Exception {
        if (args.length != 1) {
            throw new CommandExceptions.BadUsageException();
        }

        Player player = (Player) sender;
        if (player.getInventory().addItem(PlayerHeadUtils.getViaValue(args[0])).size() > 0) {
            throw new CommandExceptions.CustomCommandException(NewNanPlus.getPlugin().
                    wolfyLanguageAPI.replaceColoredKeys("$global_message.no_more_space_in_inventory$"));
        }

        NewNanPlus.getPlugin().messageManager.sendMessage(sender, NewNanPlus.getPlugin().wolfyLanguageAPI.
                replaceColoredKeys("$module_message.power_tools.skull_create_succeed$"));
    }
}
