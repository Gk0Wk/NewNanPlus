package city.newnan.newnanplus;

import city.newnan.newnanplus.exception.CommandExceptions.*;
import city.newnan.newnanplus.utility.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;

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
     * 获取插件的名字
     * @return 插件的名字
     */
    public String getName() {
        return "全局模块";
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        prefixString = wolfyLanguageAPI.replaceColoredKeys("$chat_prefix$");

        AccessFileErrorException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.access_file_error$");
        BadUsageException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.bad_usage$");
        CustomCommandException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.custom_command_error$");
        NoSuchCommandException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.no_such_command$");
        NoPermissionException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.no_permission$");
        OnlyConsoleException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.only_console$");
        PlayerMoreThanOneException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.find_more_than_one_player$");
        PlayerNotFountException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.player_not_found$");
        PlayerOfflineException.message =  wolfyLanguageAPI.replaceColoredKeys("&c$global_message.player_offline$");
        RefuseConsoleException.message = wolfyLanguageAPI.replaceColoredKeys("&c$global_message.console_selfrun_refuse$");
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
    /* 核心 */
    public city.newnan.newnanplus.NewNanPlusPlugin plugin;
    public city.newnan.newnanplus.utility.ConfigManager configManager;
    public city.newnan.newnanplus.utility.CommandManager commandManager;

    public me.wolfyscript.utilities.api.WolfyUtilities wolfyAPI;
    public me.wolfyscript.utilities.api.language.LanguageAPI wolfyLanguageAPI;
    public org.anjocaido.groupmanager.GroupManager groupManager;
    // public me.wolfyscript.utilities.api.inventory.InventoryAPI wolfyInventoryAPI;

    public org.dynmap.DynmapAPI dynmapAPI;
    /**
     * Vault 经济实例
     */
    public net.milkbowl.vault.economy.Economy vaultEco;

    /* =============================================================================================== */
    /* 全局 */
    public SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* =============================================================================================== */
    /* 模块 */

    /* NewNanPlus Town */
    public city.newnan.newnanplus.town.TownManager townManager;

    /* NewNanPlus CreateArea */
    public city.newnan.newnanplus.createarea.CreateArea createArea;

    /* NewNanPlus Player */
    public city.newnan.newnanplus.playermanager.PlayerManager playerManager;

    /* NewNanPlus DeathTrigger */
    public city.newnan.newnanplus.deathtrigger.DeathTrigger deathTrigger;

    /* NewNanPlus LagAnalyzer */
    public city.newnan.newnanplus.laganalyzer.LagAnalyzer lagAnalyzer;

    /* NewNanPlus Corn */
    public city.newnan.newnanplus.cron.Cron cron;

    /* NewNanPlus FeeFly */
    public city.newnan.newnanplus.feefly.FeeFly feeFly;
}
