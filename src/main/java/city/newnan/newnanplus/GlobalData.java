package city.newnan.newnanplus;

import city.newnan.newnanplus.exception.CommandExceptions.*;
import city.newnan.newnanplus.utility.MessageManager;
import me.wolfyscript.utilities.api.WolfyUtilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;

/**
 * NewNanPlus插件公用数据的存储类，插件内只有一份实例，每个部分都持有一份引用，以此来实现插件内通讯和持久化存储。
 */
public class GlobalData extends MessageManager implements NewNanPlusModule {
    public GlobalData(NewNanPlus plugin) {
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
     * 由于GlobalData初始化特殊，所以初始化分了三步
     */
    public void otherInit() {
        commandManager.register("titlemsg", this);
        commandManager.register("titlebroadcast", this);
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
        if (token.equals("titlemsg"))
            sendTitleMessage(args);
        else if (token.equals("titlebroadcast"))
            sendTitleBroadcast(args);
    }

    private void sendTitleMessage(String[] args) throws Exception {
        // 检查参数
        if (args.length < 2 || args.length > 3) {
            throw new BadUsageException();
        }
        // 找玩家
        Player player = plugin.getServer().getPlayer(args[0]);
        if (player == null) {
            throw new PlayerNotFountException();
        }

        // 寻找标题
        String title = null;
        if (args[1].matches("^title:")) {
            title = args[1].replaceFirst("^title:", "");
        }
        else if (args.length == 3 && args[2].matches("^title:")) {
            title = args[2].replaceFirst("^title:", "");
        }
        if (title != null)
            title = WolfyUtilities.translateColorCodes(title);

        // 寻找子标题
        String subTitle = null;
        if (args[1].matches("^subtitle:.*")) {
            subTitle = args[1].replaceFirst("^subtitle:", "");
        }
        else if (args.length == 3 && args[2].matches("^subtitle:.*")) {
            subTitle = args[2].replaceFirst("^subtitle:", "");
        }
        if (subTitle != null)
            subTitle = WolfyUtilities.translateColorCodes(subTitle);

        if (title == null && subTitle == null) {
            throw new BadUsageException();
        }

        if (((title == null) ^ (subTitle == null)) && args.length == 3) {
            throw new BadUsageException();
        }

        player.sendTitle(title, subTitle, 3, 37, 2);
    }

    private void sendTitleBroadcast(String[] args) throws Exception {
        // 检查参数
        if (args.length < 1 || args.length > 2) {
            throw new BadUsageException();
        }

        // 寻找标题
        String title = null;
        if (args[0].matches("^title:.*")) {
            title = args[0].replaceFirst("^title:", "");
        }
        else if (args.length == 2 && args[1].matches("^title:.*")) {
            title = args[1].replaceFirst("^title:", "");
        }
        if (title != null)
            title = WolfyUtilities.translateColorCodes(title);

        // 寻找子标题
        String subTitle = null;
        if (args[0].matches("^subtitle:.*")) {
            subTitle = args[0].replaceFirst("^subtitle:", "");
        }
        else if (args.length == 2 && args[1].matches("^subtitle:.*")) {
            subTitle = args[1].replaceFirst("^subtitle:", "");
        }
        if (subTitle != null)
            subTitle = WolfyUtilities.translateColorCodes(subTitle);

        if (title == null && subTitle == null) {
            throw new BadUsageException();
        }

        if (((title == null) ^ (subTitle == null)) && args.length == 2) {
            throw new BadUsageException();
        }

        String finalTitle = title;
        String finalSubTitle = subTitle;
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            if (!player.hasPermission("newnanplus.titlebroadcast.bypass"))
                player.sendTitle(finalTitle, finalSubTitle, 3, 37, 2);
        });
    }

    /* =============================================================================================== */
    /* 核心 */
    public NewNanPlus plugin;
    public city.newnan.newnanplus.utility.ConfigManager configManager;
    public city.newnan.newnanplus.utility.CommandManager commandManager;

    public me.wolfyscript.utilities.api.WolfyUtilities wolfyAPI;
    public me.wolfyscript.utilities.api.language.LanguageAPI wolfyLanguageAPI;
    public org.anjocaido.groupmanager.GroupManager groupManager;

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

    /* NewNanPlus RailExpress */
    public city.newnan.newnanplus.railexpress.RailExpress railExpress;
}
