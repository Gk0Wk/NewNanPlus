package city.newnan.newnanplus;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * NewNanPlus插件公用数据的存储类，插件内只有一份实例，每个部分都持有一份引用，以此来实现插件内通讯和持久化存储。
 */
public class NewNanPlusGlobal implements NewNanPlusModule {
    public NewNanPlusGlobal(NewNanPlusPlugin plugin, city.newnan.newnanplus.utility.ConfigManager manager) {
        this.plugin = plugin;
        this.configManager = manager;
        // 绑定控制台输出
        this.consoleLogger = plugin.getLogger();
        reloadConfig();
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        FileConfiguration config =  configManager.get("config.yml");
        globalMessage.put("NO_PERMISSION", config.getString("global-data.no-permission-msg"));
        globalMessage.put("REFUSE_CONSOLE_SELFRUN", config.getString("global-data.console-selfrun-refuse"));
        globalMessage.put("PLAYER_OFFLINE", config.getString("global-data.player-offline-msg"));
        globalMessage.put("PARAMETER_NUMBER_NOT_MATCH", config.getString("global-data.parameter-number-not-match"));
        globalMessage.put("PREFIX", config.getString("global-data.prefix"));
    }

    /* =============================================================================================== */
    /* 本体 */
    public NewNanPlusPlugin plugin;
    public NewNanPlusCommand command;
    public NewNanPlusListener listener;

    /* =============================================================================================== */
    /* 全局 */
    public me.wolfyscript.utilities.api.WolfyUtilities wolfyAPI;
    public me.wolfyscript.utilities.api.inventory.InventoryAPI wolfyInventoryAPI;
    public me.wolfyscript.utilities.api.language.LanguageAPI wolfyLanguageAPI;
    public org.dynmap.DynmapAPI dynmapAPI;

    public city.newnan.newnanplus.utility.ConfigManager configManager;

    public SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * 控制台日志实例，会自动带 <code>[NewNanPlus] </code>
     */
    public java.util.logging.Logger consoleLogger;
    /**
     * Vault 经济实例
     */
    public Economy vaultEco = null;
    /**
     * Vault 权限实例
     */
    public Permission vaultPerm = null;

    public HashMap<String, String> globalMessage = new HashMap<>();

    /* =============================================================================================== */
    /* 模块 */
    /* NewNanPlus BuildingField*/

    /* NewNanPlus Town */
    public city.newnan.newnanplus.town.TownManager townManager;

    /* NewNanPlus CreateArea **/
    public city.newnan.newnanplus.createarea.CreateArea createArea;

    /* NewNanPlus Player **/
    public city.newnan.newnanplus.playermanager.PlayerManager playerManager;

    /* NewNanPlus DeathTrigger **/
    public city.newnan.newnanplus.deathtrigger.DeathTrigger deathTrigger;

    /* NewNanPlus LagAnalyzer */
    public city.newnan.newnanplus.laganalyzer.LagAnalyzer lagAnalyzer;

    /* NewNanPlus Corn */
    public city.newnan.newnanplus.cron.Cron cron;

    /* NewNanPlus FeeFly **/
    public city.newnan.newnanplus.feefly.FeeFly feeFly;


    /* =============================================================================================== */
    /* 方法 */
    /**
     * 向控制台(其实是向JAVA日志)输出INFO日志
     * @param msg 要发送的消息
     */
    public void printINFO(String msg) {
        consoleLogger.info(msg);
    }

    /**
     * 向控制台(其实是向JAVA日志)输出WARN日志
     * @param msg 要发送的消息
     */
    public void printWARN(String msg) {
        consoleLogger.warning(msg);
    }

    /**
     * 向控制台(其实是向JAVA日志)输出ERROR日志
     * @param msg 要发送的消息
     */
    public void printERROR(String msg) {
        consoleLogger.severe(msg);
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
                    globalMessage.get("PREFIX") + msg);
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
