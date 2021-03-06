package city.newnan.newnanplus.playermanager;

import city.newnan.api.config.ConfigManager;
import city.newnan.api.config.ConfigUtil;
import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions;
import city.newnan.newnanplus.exception.CommandExceptions.BadUsageException;
import city.newnan.newnanplus.exception.CommandExceptions.PlayerMoreThanOneException;
import city.newnan.newnanplus.exception.CommandExceptions.PlayerNotFountException;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import city.newnan.newnanplus.utility.PlayerConfig;
import me.lucko.helper.config.ConfigurationNode;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class PlayerManager implements Listener, NewNanPlusModule {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    private Group newbiesGroup;
    private Group playersGroup;
    private Group judgementalGroup;
    private OverloadedWorldHolder workWorldsPermissionHandler;
    /**
     * 构造函数
     */
    public PlayerManager() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManagers.get("config.yml").getNode("module-playermanager", "enable").getBoolean(false)) {
            throw new ModuleOffException();
        }
        reloadConfig();

        // 注册监听函数
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.commandManager.register("allow", this);
        plugin.commandManager.register("judgemental", this);
        plugin.commandManager.register("pushtask", this);
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        // 获取配置实例
        try {
            plugin.configManagers.reload("newbies_list.yml");
            // 加载配置内容
            ConfigurationNode config = plugin.configManagers.get("config.yml").getNode("module-playermanager");
            workWorldsPermissionHandler = plugin.groupManager.getWorldsHolder().
                    getWorldData(config.getNode("world-group").getString());
            newbiesGroup = workWorldsPermissionHandler.getGroup(config.getNode("newbies-group").getString());
            playersGroup = workWorldsPermissionHandler.getGroup(config.getNode("player-group").getString());
            judgementalGroup = workWorldsPermissionHandler.getGroup(config.getNode("judgemental-group").getString());
        } catch (IOException | ConfigManager.UnknownConfigFileFormatException e) {
            e.printStackTrace();
        }
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
    public void executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {
        switch (token) {
            case "allow":
                allowNewbieToPlayer(sender, args);
                break;
            case "judgemental":
                toggleJudgementalMode(sender);
                break;
            case "pushtask":
                pushTaskCommand(sender, args);
                break;
        }
    }

    /**
     * 玩家登录时触发的方法
     * @param event 玩家登录事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws Exception {
        Player player = event.getPlayer();
        joinCheck(player);
        executeTaskQueue(player);
        showUpdateLog(player);
        PlayerConfig.getPlayerConfig(player).commit();
    }

    /**
     * 玩家退出时触发的方法
     * @param event 玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) throws Exception {
        // 将缓存文件卸载
        PlayerConfig.unloadPlayerConfig(event.getPlayer());
    }

    /**
     * 根据名字返回一个且仅有一个玩家(不一定要在线)，如果有多个重名玩家就报错
     * @param playerName 玩家名称
     * @return 玩家实例
     * @throws Exception 如果找不到玩家或者找到多个玩家就会抛出的异常
     */
    public OfflinePlayer findOnePlayerByName(String playerName) throws Exception {
        OfflinePlayer player;
        try {
            // 查找对应的玩家
            List<Player> players = plugin.getServer().matchPlayer(playerName);
            // 检查玩家数量
            if (players.size() == 0) {
                throw new PlayerNotFountException();
            } else if (players.size() > 1) {
                throw new PlayerMoreThanOneException();
            }
            player = players.get(0);
        } catch (Exception e) {
            if (e instanceof PlayerNotFountException) {
                OfflinePlayer[] players = Arrays.stream(plugin.getServer().getOfflinePlayers()).
                        filter(offlinePlayer -> Objects.equals(offlinePlayer.getName(), playerName)).toArray(OfflinePlayer[]::new);
                if (players.length > 1) {
                    throw new PlayerMoreThanOneException();
                }
                if (players.length == 0) {
                    throw new PlayerNotFountException();
                }
                player = players[0];
            } else {
                throw e;
            }
        }
        return player;
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

        OfflinePlayer player;
        try {
            // 寻找目标玩家
            player = ((city.newnan.newnanplus.playermanager.PlayerManager)plugin.
                    getModule(city.newnan.newnanplus.playermanager.PlayerManager.class)).findOnePlayerByName(args[0]);
        } catch (Exception e) {
            if (e instanceof PlayerNotFountException)
                player = null;
            else
                throw e;
        }

        // 是否需要刷新新人名单
        boolean need_refresh = false;

        ConfigurationNode newbiesList = plugin.configManagers.get("newbies_list.yml");

        if (player != null) {
            // 如果能找到玩家，说明玩家至少已经进过一次游戏了，就直接赋予权限
            // 但是要检查一下原来所在的组
            if (workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).getGroup().equals(newbiesGroup)) {
                workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).setGroup(playersGroup);
                // 获取未通过的新人组的List
                List<String> list_not = new ArrayList<>(ConfigUtil.setListIfNull(newbiesList.getNode("not-passed-newbies")).getList(Object::toString));
                // 如果未通过里有这个玩家，那就去掉
                if (list_not.contains(player.getName())) {
                    list_not.remove(player.getName());
                    newbiesList.getNode("not-passed-newbies").setValue(list_not);
                    need_refresh = true;
                }
                plugin.messageManager.printf(sender, "$module_message.player_manager.allow_succeed$", player.getName());
            } else {
                plugin.messageManager.printf(sender, "$module_message.player_manager.not_newbie_already$", player.getName());
                if (player.isOnline()) {
                    plugin.messageManager.printf(player.getPlayer(), "$module_message.player_manager.you_are_nolonger_newbie$");
                } else {
                    ((city.newnan.newnanplus.playermanager.PlayerManager)plugin.
                            getModule(city.newnan.newnanplus.playermanager.PlayerManager.class)).
                            pushTask(player, plugin.messageManager.sprintf(
                                    "nnp msg {0} $module_message.player_manager.you_are_nolonger_newbie$", player.getName()));
                    ((city.newnan.newnanplus.playermanager.PlayerManager)plugin.
                            getModule(city.newnan.newnanplus.playermanager.PlayerManager.class)).
                            pushTask(player, plugin.messageManager.sprintf(
                                    "nnp msg {0} $module_message.player_manager.you_are_nolonger_newbie$", player.getName()));
                }
            }
        }

        // 如果玩家不在线或不存在，就存到配置里
        else {
            // 获取未通过的新人组的List
            List<String> list_not = new ArrayList<>(ConfigUtil.setListIfNull(newbiesList.getNode("not-passed-newbies")).getList(Object::toString));
            if (list_not.contains(args[0])) {
                list_not.remove(args[0]);
                newbiesList.getNode("not-passed-newbies").setValue(list_not);
                need_refresh = true;
            }
            // 获取已通过的新人组的List
            List<String> list_yet = new ArrayList<>(ConfigUtil.setListIfNull(newbiesList.getNode("yet-passed-newbies")).getList(Object::toString));
            if (!list_yet.contains(args[0])) {
                list_yet.add(args[0]);
                newbiesList.getNode("yet-passed-newbies").setValue(list_yet);
                need_refresh = true;
            }
            plugin.messageManager.printf(sender, "$module_message.player_manager.allow_later$", args[0]);
        }

        if (need_refresh) {
            plugin.configManagers.save("newbies_list.yml");
        }
    }

    /**
     * 切换风纪委员的状态
     * @param player 待切换状态的玩家
     * @param toJudgemental 是否切换至风纪委员状态，false则反之
     */
    public void changeJudgementalMode(Player player, boolean toJudgemental) {
        try {
            if (toJudgemental) {
                player.setGameMode(GameMode.SPECTATOR);
                plugin.getServer().broadcastMessage(plugin.messageManager.sprintf(Objects.requireNonNull(plugin.configManagers.get("config.yml").
                                getNode("module-playermanager", "judgemental-fake-quit-message").getString()), player.getName()));
            } else {
                player.setGameMode(GameMode.SURVIVAL);
                plugin.getServer().broadcastMessage(plugin.messageManager.sprintf(Objects.requireNonNull(plugin.configManagers.get("config.yml").
                        getNode("module-playermanager", "judgemental-fake-join-message").getString()), player.getName()));
            }
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "vanish " + player.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 玩家切换世界时触发，用于让风纪委员与其状态一致
     * @param event 玩家切换世界的事件
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        OverloadedWorldHolder sourceWorldsHolder = plugin.groupManager.getWorldsHolder().
                getWorldData(event.getFrom().toString());
        OverloadedWorldHolder targetWorldsHolder = plugin.groupManager.getWorldsHolder().
                getWorldData(Objects.requireNonNull(event.getPlayer().getLocation().getWorld()).toString());
        boolean sourceJudgemental = sourceWorldsHolder.getUser(
                event.getPlayer().getUniqueId().toString()).getGroup().equals(judgementalGroup);
        boolean targetJudgemental = targetWorldsHolder.getUser(
                event.getPlayer().getUniqueId().toString()).getGroup().equals(judgementalGroup);

        // 风纪 -> 非风纪
        if (sourceJudgemental && !targetJudgemental) {
            changeJudgementalMode(event.getPlayer(), false);
        }
        // 非风纪 -> 风纪
        else if (!sourceJudgemental && targetJudgemental) {
            changeJudgementalMode(event.getPlayer(), true);
        }
    }

    /**
     * 切换玩家风纪委员状态的命令
     * @param sender 命令执行者
     * @throws Exception 命令异常
     */
    public void toggleJudgementalMode(CommandSender sender) throws Exception {
        if (sender instanceof ConsoleCommandSender) {
            throw new CommandExceptions.RefuseConsoleException();
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            // 如果玩家当前是风纪委员组，就切换回玩家组
            if (workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).getGroup().equals(judgementalGroup)) {
                workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).setGroup(playersGroup);
                changeJudgementalMode(player, false);
            } else {
                // 否则就进入风纪委员组
                workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).setGroup(judgementalGroup);
                changeJudgementalMode(player, true);
            }
        }
    }

    public void executeTaskQueue(Player player) throws Exception {
        final ConsoleCommandSender sender = plugin.getServer().getConsoleSender();
        PlayerConfig playerConfig = PlayerConfig.getPlayerConfig(player);
        playerConfig.getLoginTaskQueue().forEach(command -> {
            plugin.messageManager.info("Run command: " + command);
            plugin.getServer().dispatchCommand(sender, command);
        });
        playerConfig.getLoginTaskQueue().clear();
    }

    public void pushTaskCommand(CommandSender sender, String[] args) throws Exception {
        if (args.length < 2) {
            throw new BadUsageException();
        }
        // 找到这个玩家，异常将会抛出
        OfflinePlayer player = findOnePlayerByName(args[0]);
        String command = StringUtils.join(args, ' ', 1, args.length);
        pushTask(player, command);
        plugin.messageManager.printf(sender, "$module_message.player_manager.pushtask_succeed$", player.getName(), command);
    }

    public void pushTask(OfflinePlayer player, String command) throws Exception {
        PlayerConfig playerConfig = PlayerConfig.getPlayerConfig(player);
        playerConfig.getLoginTaskQueue().add(command);
        playerConfig.commit();
    }

    public void pushTask(OfflinePlayer player, List<String> commands) throws Exception {
        PlayerConfig playerConfig = PlayerConfig.getPlayerConfig(player);
        commands.forEach(command -> playerConfig.getLoginTaskQueue().add(command));
        playerConfig.commit();
    }

    /**
     * 检查玩家的权限，如果玩家是新人则通知其去做问卷；如果已在验证新人名单里就直接送入玩家组
     * @param player 待检测的玩家实例
     * @throws Exception 指令异常
     */
    public void joinCheck(Player player) throws Exception {
        ConfigurationNode newbiesList = plugin.configManagers.get("newbies_list.yml");
        boolean need_refresh = false;
        // 获取已通过的新人组的List
        List<String> list_yet = new ArrayList<>(ConfigUtil.setListIfNull(newbiesList.getNode("yet-passed-newbies")).getList(Object::toString));
        // 如果是新人组的话
        if (workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).getGroup().equals(newbiesGroup)) {
            if (list_yet.contains(player.getName())) {
                // 查看玩家是否在已通过新人组，将玩家移入玩家权限组，并更新
                workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).setGroup(playersGroup);
                list_yet.remove(player.getName());
                newbiesList.getNode("yet-passed-newbies").setValue(list_yet);
                need_refresh = true;
                plugin.messageManager.printf(player, "$module_message.player_manager.you_are_nolonger_newbie$");
            } else {
                plugin.messageManager.printf(player, "$module_message.player_manager.you_are_newbie$");
                player.sendTitle(
                        plugin.messageManager.sprintf("$module_message.player_manager.welcome_title$"),
                        plugin.messageManager.sprintf("$module_message.player_manager.welcome_subtitle$"),
                        3, 97, 5);
                // 获取未通过的新人组的List
                List<String> list_not = new ArrayList<>(ConfigUtil.setListIfNull(newbiesList.getNode("not-passed-newbies")).getList(Object::toString));
                if (!list_not.contains(player.getName())) {
                    // 查看玩家是否在未通过新人组，没加入就加入，并更新
                    list_not.add(player.getName());
                    newbiesList.getNode("not-passed-newbies").setValue(list_not);
                    need_refresh = true;
                }
            }
        } else {
            if (list_yet.contains(player.getName())){
                // 看看是不是已经成为玩家但是还在名单里
                list_yet.remove(player.getName());
                newbiesList.getNode("yet-passed-newbies").setValue(list_yet);
                need_refresh = true;
            }
        }
        if (need_refresh) {
            plugin.configManagers.save("newbies_list.yml");
        }
    }

    public void showUpdateLog(Player player) throws Exception {
        final String header = plugin.messageManager.sprintf("$module_message.player_manager.updatelog_head$") + "§r\n";
        final String title = plugin.messageManager.sprintf("$module_message.player_manager.updatelog_title$");

        PlayerConfig playerConfig = PlayerConfig.getPlayerConfig(player);
        long lastTime = playerConfig.getLastLoginTime();

        ConfigurationNode updateLog = plugin.configManagers.reload("update_log.yml");
        List<String> logs = new ArrayList<>();
        logs.add(header);


        updateLog.getNode("logs").getChildrenMap().forEach((date, node) -> {
            try {
                if (plugin.dateFormatter.parse((String) date).getTime() > lastTime) {
                    logs.add(node.getString());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        if (logs.size() > 1) {
            player.openBook(((city.newnan.newnanplus.powertools.PowerTools)
                    plugin.getModule(city.newnan.newnanplus.powertools.PowerTools.class))
                    .createBook(title, "NewNanCity", null, BookMeta.Generation.ORIGINAL, logs));
        }
        //
        playerConfig.setLastLoginTime(System.currentTimeMillis());
    }
}
