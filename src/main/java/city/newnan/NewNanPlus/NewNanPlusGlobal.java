package city.newnan.NewNanPlus;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * NewNanPlus插件公用数据的存储类，插件内只有一份实例，每个部分都持有一份引用，以此来实现插件内通讯和持久化存储。
 */
public class NewNanPlusGlobal {
    /* =============================================================================================== */
    /* 本体 */
    public NewNanPlusPlugin Plugin;
    public NewNanPlusCommand Command;
    public NewNanPlusListener Listener;
    // public NewNanPlusExecute Execute;


    /* =============================================================================================== */
    /* 全局 */
    public me.wolfyscript.utilities.api.WolfyUtilities WolfyAPI;
    public me.wolfyscript.utilities.api.inventory.InventoryAPI WolfyInventoryAPI;
    public me.wolfyscript.utilities.api.language.LanguageAPI WolfyLanguageAPI;

    public SimpleDateFormat DateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public FileConfiguration Config = null;
    /**
     * 控制台日志实例，会自动带 <code>[NewNanPlus] </code>
     */
    public java.util.logging.Logger ConsoleLogger;
    /**
     * Vault 经济实例
     */
    public Economy VaultEco = null;
    /**
     * Vault 权限实例
     */
    public Permission VaultPerm = null;


    /* =============================================================================================== */
    /* 模块 */
    /* NewNanPlus BuildingField*/
    // public YamlConfiguration BuildingField;

    /* NewNanPlus Town */
    public ConcurrentHashMap<UUID, city.newnan.NewNanPlus.Town.Town> Towns =
            new ConcurrentHashMap<UUID, city.newnan.NewNanPlus.Town.Town>();
    public city.newnan.NewNanPlus.Town.TownCommand TownCommand;

    /* NewNanPlus CreateArea **/
    public YamlConfiguration CreateArea;
    public city.newnan.NewNanPlus.CreateArea.CACommand CACommand;


    /* NewNanPlus Player **/
    public YamlConfiguration NewbiesList;
    public HashMap<String, UUID> ReversePlayerList = new HashMap<String, UUID>();
    public city.newnan.NewNanPlus.Player.PlayerCommand PlayerCommand;


    /* NewNanPlus DeathTrigger **/
    public city.newnan.NewNanPlus.DeathTrigger.DTCommand DTCommand;


    /* NewNanPlus LaggAnalyzer */
    public HashMap<String, Integer> HopperMap = new HashMap<String, Integer>();
    public city.newnan.NewNanPlus.LaggAnalyzer.LACommand LACommand;


    /* NewNanPlus Corn */
    public city.newnan.NewNanPlus.Corn.CornCommand CornCommand;


    /* NewNanPlus Fly **/
    /** 正在飞行中的玩家，ConcurrentHashMap具有高并发性 */
    public ConcurrentHashMap<Player, city.newnan.NewNanPlus.Fly.FlyingPlayer> FlyingPlayers =
            new ConcurrentHashMap<Player, city.newnan.NewNanPlus.Fly.FlyingPlayer>();
    public city.newnan.NewNanPlus.Fly.FlyCommand FlyCommand;
    public city.newnan.NewNanPlus.Fly.FlySchedule FlySchedule;


    /* =============================================================================================== */
    /* 方法 */
    /**
     * 向控制台(其实是向JAVA日志)输出INFO日志
     * @param msg 要发送的消息
     */
    public void printINFO(String msg) {
        ConsoleLogger.info(msg);
    }

    /**
     * 向控制台(其实是向JAVA日志)输出WARN日志
     * @param msg 要发送的消息
     */
    public void printWARN(String msg) {
        ConsoleLogger.warning(msg);
    }

    /**
     * 向控制台(其实是向JAVA日志)输出ERROR日志
     * @param msg 要发送的消息
     */
    public void printERROR(String msg) {
        ConsoleLogger.severe(msg);
    }
    /**
     * 给玩家发送有样式码的信息
     * @param player 玩家实例
     * @param msg 信息内容
     * @param prefix 是否带有前缀
     */
    public void sendPlayerMessage(Player player, String msg, boolean prefix) {
        if (prefix) {
            String _msg = ChatColor.translateAlternateColorCodes('&',
                    Config.getString("global-data.prefix") + msg);
            player.sendMessage(_msg);
        } else {
            String _msg = ChatColor.translateAlternateColorCodes('&', msg);
            player.sendMessage(_msg);
        }
    }

    /**
     * 给玩家发送有样式码的信息
     * @param player 玩家实例
     * @param msg 信息内容
     */
    public void sendPlayerMessage(Player player, String msg) {
        sendPlayerMessage(player, msg, true);
    }

    /**
     * 在某个玩家的ActionBar上显示消息
     * @param player 要显示的玩家
     * @param msg 要显示的信息
     */
    public void sendPlayerActionBar(Player player, String msg) {
        String _msg = ChatColor.translateAlternateColorCodes('&', msg);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(_msg));
    }

    /**
     * 给对象发送一条消息
     * @param sender 发送的对象
     * @param msg 发送的消息
     */
    public void sendMessage(CommandSender sender, String msg) {
        if (sender instanceof Player) {
            sendPlayerMessage((Player) sender, msg);
        } else if (sender instanceof ConsoleCommandSender) {
            printINFO(msg);
        }
    }
}
