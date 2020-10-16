package city.newnan.newnanplus;

import city.newnan.newnanplus.utility.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * NewNanPlus插件公用数据的存储类，插件内只有一份实例，每个部分都持有一份引用，以此来实现插件内通讯和持久化存储。
 */
public class NewNanPlusGlobal extends MessageManager implements NewNanPlusModule {
    public NewNanPlusGlobal(NewNanPlusPlugin plugin, city.newnan.newnanplus.utility.ConfigManager manager) {
        // 绑定控制台输出
        super(plugin.getLogger(), "");

        this.plugin = plugin;
        this.configManager = manager;

        reloadConfig();
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        FileConfiguration config =  configManager.get("config.yml");

        globalMessage.put("NO_PERMISSION", config.getString("global-data.no-permission-msg"));
        globalMessage.put("REFUSE_CONSOLE_SELFRUN", config.getString("global-data.console-selfrun-refuse-msg"));
        globalMessage.put("PLAYER_OFFLINE", config.getString("global-data.player-offline-msg"));
        globalMessage.put("NO_SUCH_COMMAND", config.getString("global-data.no-such-command-msg"));
        globalMessage.put("BAD_USAGE", config.getString("global-data.bad-usage-msg"));
        globalMessage.put("ONLY_CONSOLE", config.getString("global-data.only-console-msg"));
        globalMessage.put("EXECUTE_ERROR", config.getString("global-data.command-execute-error-msg"));

        globalMessage.put("PREFIX", config.getString("global-data.prefix"));
        prefixString = config.getString("global-data.prefix");
    }

    /**
     * 执行某个命令
     *
     * @param sender  发送指令者的实例
     * @param command 被执行的指令实例
     * @param token   指令的标识字符串
     * @param args    指令的参数
     */
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {

    }

    /* =============================================================================================== */
    /* 本体 */
    public NewNanPlusPlugin plugin;
    public NewNanPlusListener listener;

    /* =============================================================================================== */
    /* 全局 */
    public me.wolfyscript.utilities.api.WolfyUtilities wolfyAPI;
    public me.wolfyscript.utilities.api.inventory.InventoryAPI wolfyInventoryAPI;
    public me.wolfyscript.utilities.api.language.LanguageAPI wolfyLanguageAPI;
    public org.dynmap.DynmapAPI dynmapAPI;

    public city.newnan.newnanplus.utility.ConfigManager configManager;
    public city.newnan.newnanplus.utility.CommandManager commandManager;

    public SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * Vault 经济实例
     */
    public net.milkbowl.vault.economy.Economy vaultEco;
    /**
     * Vault 权限实例
     */
    public net.milkbowl.vault.permission.Permission vaultPerm;

    public HashMap<String, String> globalMessage = new HashMap<>();

    /* =============================================================================================== */
    /* 模块 */
    /* NewNanPlus BuildingField*/

    /* NewNanPlus Town */
    // public city.newnan.newnanplus.town.TownManager townManager;

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
}
