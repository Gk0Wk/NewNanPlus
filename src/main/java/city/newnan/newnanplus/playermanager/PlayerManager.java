package city.newnan.newnanplus.playermanager;

import city.newnan.newnanplus.NewNanPlusGlobal;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions.BadUsageException;
import city.newnan.newnanplus.exception.CommandExceptions.PlayerMoreThanOneException;
import city.newnan.newnanplus.exception.CommandExceptions.PlayerNotFountException;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayerManager implements Listener, NewNanPlusModule {
    /**
     * 持久化访问全局数据
     */
    private final NewNanPlusGlobal globalData;

    private String newbiesGroup;
    private String playersGroup;
    private String workWorldsGroup;

    /**
     * 构造函数
     * @param globalData 全局实例
     */
    public PlayerManager(NewNanPlusGlobal globalData) throws Exception {
        this.globalData = globalData;
        if (!globalData.configManager.get("config.yml").getBoolean("module-playermanager.enable", false)) {
            throw new ModuleOffException();
        }
        reloadConfig();

        // 注册监听函数
        this.globalData.plugin.getServer().getPluginManager().registerEvents(this, this.globalData.plugin);

        globalData.playerManager = this;
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        // 获取配置实例
        globalData.configManager.reload("newbies_list.yml");
        // 加载配置内容
        FileConfiguration config = globalData.configManager.get("config.yml");
        newbiesGroup = config.getString("module-playermanager.newbies-group");
        playersGroup = config.getString("module-playermanager.player-group");
        workWorldsGroup = config.getString("module-playermanager.world-group");

        globalData.commandManager.register("allow", this);
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
        if (token.equals("allow"))
            allowNewbieToPlayer(sender, args);
    }

    /**
     * 玩家登录时触发的方法
     * @param event 玩家登录事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws Exception {
        joinCheck(event.getPlayer());
    }

    /**
     * 根据名字返回一个且仅有一个玩家(不一定要在线)，如果有多个重名玩家就报错
     * @param playerName 玩家名称
     * @return 玩家实例
     * @throws Exception 如果找不到玩家或者找到多个玩家就会抛出的异常
     */
    public Player findOnePlayerByName(String playerName) throws Exception {
        // 查找对应的玩家
        List<Player> players = globalData.plugin.getServer().matchPlayer(playerName);
        // 检查玩家数量
        if (players.size() == 0) {
            throw new PlayerNotFountException();
        } else if (players.size() > 1) {
            throw new PlayerMoreThanOneException();
        }
        return players.get(0);
    }

    /**
     * /nnp allow指令的实现，将一个玩家纳入已验证新人名单中，如果玩家已经在线，那么就直接赋予玩家权限
     * @param sender 命令的发送者
     * @param args 命令的参数，包括allow
     */
    public void allowNewbieToPlayer(CommandSender sender, String[] args) throws Exception {
        // 检查参数
        if (args.length < 1) {
            throw new BadUsageException();
        }

        // 寻找目标玩家
        Player player = globalData.plugin.getServer().getPlayer(args[0]);
        // 是否需要刷新新人名单
        boolean need_refresh = false;

        FileConfiguration newbiesList = globalData.configManager.get("newbies_list.yml");

        // 如果玩家不在线或不存在，就存到配置里
        if (player == null) {
            // 获取未通过的新人组的List
            List<String> list_not = newbiesList.getStringList("not-passed-newbies");
            if (list_not.contains(args[0])) {
                list_not.remove(args[0]);
                newbiesList.set("not-passed-newbies", list_not);
                need_refresh = true;
            }
            // 获取已通过的新人组的List
            List<String> list_yet = newbiesList.getStringList("yet-passed-newbies");
            if (!list_yet.contains(args[0])) {
                list_yet.add(args[0]);
                newbiesList.set("yet-passed-newbies", list_yet);
                need_refresh = true;
            }
            globalData.sendMessage(sender,
                    globalData.wolfyLanguageAPI.replaceKeys("$module_message.player_manager.allow_later$"));
        } else {
            // 如果玩家在线，就直接赋予权限
            // 但是要检查一下原来所在的组
            if (globalData.vaultPerm.getPrimaryGroup(player).equalsIgnoreCase(newbiesGroup)) {
                globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                        "manuadd " + player.getName() + " " + playersGroup + " " + workWorldsGroup);
                // 获取未通过的新人组的List
                List<String> list_not = newbiesList.getStringList("not-passed-newbies");
                // 如果未通过里有这个玩家，那就去掉
                if (list_not.contains(player.getName())) {
                    list_not.remove(player.getName());
                    newbiesList.set("not-passed-newbies", list_not);
                    need_refresh = true;
                }
                globalData.sendMessage(sender,
                        globalData.wolfyLanguageAPI.replaceKeys("$module_message.player_manager.allow_succeed$"));
            } else {
                globalData.sendMessage(sender,
                        globalData.wolfyLanguageAPI.replaceKeys("$module_message.player_manager.not_newbie_already$"));
            }
        }
        if (need_refresh) {
            globalData.configManager.save("newbies_list.yml");
        }
    }

    /**
     * 检查玩家的权限，如果玩家是新人则通知其去做问卷；如果已在验证新人名单里就直接送入玩家组
     * @param player 待检测的玩家实例
     */
    public void joinCheck(Player player) throws Exception {
        FileConfiguration newbiesList = globalData.configManager.get("newbies_list.yml");
        boolean need_refresh = false;
        // 获取已通过的新人组的List
        List<String> list_yet = newbiesList.getStringList("yet-passed-newbies");
        // 如果是新人组的话
        if (globalData.vaultPerm.getPrimaryGroup(player).equalsIgnoreCase(newbiesGroup)) {
            if (list_yet.contains(player.getName())) {
                // 查看玩家是否在已通过新人组，将玩家移入玩家权限组，并更新
                globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                        "manuadd " + player.getName() + " " + playersGroup + " " + workWorldsGroup);
                list_yet.remove(player.getName());
                newbiesList.set("yet-passed-newbies", list_yet);
                need_refresh = true;
            } else {
                globalData.sendPlayerMessage(player, globalData.wolfyLanguageAPI.
                        replaceKeys("$module_message.player_manager.you_are_newbie$"));
                player.sendTitle(
                        globalData.wolfyLanguageAPI.replaceColoredKeys("$module_message.player_manager.welcome_title$"),
                        globalData.wolfyLanguageAPI.replaceColoredKeys("$module_message.player_manager.welcome_subtitle$"),
                        3, 37, 5);
                // 获取未通过的新人组的List
                List<String> list_not = newbiesList.getStringList("not-passed-newbies");
                if (!list_not.contains(player.getName())) {
                    // 查看玩家是否在未通过新人组，没加入就加入，并更新
                    list_not.add(player.getName());
                    newbiesList.set("not-passed-newbies", list_not);
                    need_refresh = true;
                }
            }
        } else {
            if (list_yet.contains(player.getName())){
                // 看看是不是已经成为玩家但是还在名单里
                list_yet.remove(player.getName());
                newbiesList.set("yet-passed-newbies", list_yet);
                need_refresh = true;
            }
        }
        if (need_refresh) {
            globalData.configManager.save("newbies_list.yml");
        }
    }
}
