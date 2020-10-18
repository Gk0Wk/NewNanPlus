package city.newnan.newnanplus;

import city.newnan.newnanplus.exception.CommandExceptions.*;
import city.newnan.newnanplus.utility.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * NewNanPlus插件公用数据的存储类，插件内只有一份实例，每个部分都持有一份引用，以此来实现插件内通讯和持久化存储。
 */
public class NewNanPlusGlobal extends MessageManager implements NewNanPlusModule {
    public NewNanPlusGlobal(NewNanPlusPlugin plugin) {
        // 绑定控制台输出
        super(plugin.getLogger(), "");
        this.plugin = plugin;
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        NoPermissionException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.no_permission$");
        RefuseConsoleException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.console_selfrun_refuse$");
        PlayerOfflineException.message =  wolfyLanguageAPI.replaceColoredKeys("&c$global_message.player_offline$");
        PlayerNotFountException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.player_not_found$");
        PlayerMoreThanOneException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.find_more_than_one_player$");
        NoSuchCommandException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.no_such_command$");
        BadUsageException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.bad_usage$");
        OnlyConsoleException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.only_console$");
        CustomCommandException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.custom_command_error$");

        prefixString = wolfyLanguageAPI.replaceColoredKeys("$chat_prefix$");
        globalMessage.put("PREFIX",prefixString);
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


    /* =============================================================================================== */
    /* 全局 */
    public me.wolfyscript.utilities.api.WolfyUtilities wolfyAPI;
    public me.wolfyscript.utilities.api.inventory.InventoryAPI wolfyInventoryAPI;
    public me.wolfyscript.utilities.api.language.LanguageAPI wolfyLanguageAPI;
    public org.dynmap.DynmapAPI dynmapAPI;

    public city.newnan.newnanplus.utility.ConfigManager configManager;
    public city.newnan.newnanplus.utility.CommandManager commandManager;

    /**
     * Vault 经济实例
     */
    public net.milkbowl.vault.economy.Economy vaultEco;
    /**
     * Vault 权限实例
     */
    public net.milkbowl.vault.permission.Permission vaultPerm;

    public HashMap<String, String> globalMessage = new HashMap<>();
    public SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* =============================================================================================== */
    /* 模块 */
    public city.newnan.newnanplus.NewNanPlusPlugin plugin;

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
