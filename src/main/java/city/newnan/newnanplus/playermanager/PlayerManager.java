package city.newnan.newnanplus.playermanager;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions;
import city.newnan.newnanplus.exception.CommandExceptions.BadUsageException;
import city.newnan.newnanplus.exception.CommandExceptions.PlayerMoreThanOneException;
import city.newnan.newnanplus.exception.CommandExceptions.PlayerNotFountException;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import me.wolfyscript.utilities.api.WolfyUtilities;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        if (!plugin.configManager.get("config.yml").getBoolean("module-playermanager.enable", false)) {
            throw new ModuleOffException();
        }
        reloadConfig();

        // 注册监听函数
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        // 获取配置实例
        plugin.configManager.reload("newbies_list.yml");
        // 加载配置内容
        FileConfiguration config = plugin.configManager.get("config.yml");
        workWorldsPermissionHandler = plugin.groupManager.getWorldsHolder().
                getWorldData(config.getString("module-playermanager.world-group"));
        newbiesGroup = workWorldsPermissionHandler.getGroup(config.getString("module-playermanager.newbies-group"));
        playersGroup = workWorldsPermissionHandler.getGroup(config.getString("module-playermanager.player-group"));
        judgementalGroup = workWorldsPermissionHandler.getGroup(config.getString("module-playermanager.judgemental-group"));

        plugin.commandManager.register("allow", this);
        plugin.commandManager.register("judgemental", this);
        plugin.commandManager.register("pushtask", this);
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
        joinCheck(event.getPlayer());
        touchPlayer(event.getPlayer());
        emptyTaskQueue(event.getPlayer());
        showUpdateLog(event.getPlayer());

    }

    /**
     * 玩家退出时触发的方法
     * @param event 玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) throws Exception {
        String playerConfigPath = "player/" + event.getPlayer().getUniqueId().toString() + ".yml";
        plugin.configManager.unload(playerConfigPath, true);
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

        // 寻找目标玩家
        OfflinePlayer player = ((city.newnan.newnanplus.playermanager.PlayerManager)plugin.
                getModule(city.newnan.newnanplus.playermanager.PlayerManager.class)).findOnePlayerByName(args[0]);

        // 是否需要刷新新人名单
        boolean need_refresh = false;

        FileConfiguration newbiesList = plugin.configManager.get("newbies_list.yml");

        if (player != null) {
            // 如果能找到玩家，说明玩家至少已经进过一次游戏了，就直接赋予权限
            // 但是要检查一下原来所在的组
            if (workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).getGroup().equals(newbiesGroup)) {
                workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).setGroup(playersGroup);
                // 获取未通过的新人组的List
                List<String> list_not = newbiesList.getStringList("not-passed-newbies");
                // 如果未通过里有这个玩家，那就去掉
                if (list_not.contains(player.getName())) {
                    list_not.remove(player.getName());
                    newbiesList.set("not-passed-newbies", list_not);
                    need_refresh = true;
                }
                plugin.messageManager.sendMessage(sender,
                        plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.player_manager.allow_succeed$"));
            } else {
                plugin.messageManager.sendMessage(sender,
                        plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.player_manager.not_newbie_already$"));
            }
        }

        // 如果玩家不在线或不存在，就存到配置里
        else {
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
            plugin.messageManager.sendMessage(sender,
                    plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.player_manager.allow_later$"));
        }

        if (need_refresh) {
            plugin.configManager.save("newbies_list.yml");
        }
    }

    /**
     * 切换风纪委员的状态
     * @param player 待切换状态的玩家
     * @param toJudgemental 是否切换至风纪委员状态，false则反之
     */
    public void changeJudgementalMode(Player player, boolean toJudgemental) {
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "vanish " + player.getName());
        if (toJudgemental) {
            player.setGameMode(GameMode.SPECTATOR);
            plugin.getServer().broadcastMessage(WolfyUtilities.translateColorCodes(MessageFormat.format(
                    Objects.requireNonNull(plugin.configManager.get("config.yml").
                            getString("module-playermanager.judgemental-fake-quit-message")),
                    player.getName())));
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            plugin.getServer().broadcastMessage(WolfyUtilities.translateColorCodes(MessageFormat.format(
                    Objects.requireNonNull(plugin.configManager.get("config.yml").
                            getString("module-playermanager.judgemental-fake-join-message")),
                    player.getName())));
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

    public void emptyTaskQueue(OfflinePlayer player) throws Exception {
        final ConsoleCommandSender sender = plugin.getServer().getConsoleSender();
        String filePath = "player/" + player.getUniqueId().toString() + ".yml";
        FileConfiguration playerConfig = plugin.configManager.get(filePath);
        if (playerConfig.isList("login-task-queue")) {
            List<String> commands = playerConfig.getStringList("login-task-queue");
            commands.forEach(command -> {
                plugin.messageManager.printINFO("Run command: " + command);
                plugin.getServer().dispatchCommand(sender, command);
            });
            commands.clear();
            playerConfig.set("login-task-queue", commands);
            plugin.configManager.save(filePath);
        }
    }

    public void pushTaskCommand(CommandSender sender, String[] args) throws Exception {
        if (args.length < 2) {
            throw new BadUsageException();
        }
        // 找到这个玩家，异常将会抛出
        OfflinePlayer player = findOnePlayerByName(args[0]);
        String command = StringUtils.join(args, ' ', 1, args.length);
        pushTask(player, command);
        plugin.messageManager.sendMessage(sender, MessageFormat.format(
                plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.player_manager.pushtask_succeed$"),
                player.getName(), command));
    }

    public void pushTask(OfflinePlayer player, String command) throws Exception {
        String filePath = "player/" + player.getUniqueId().toString() + ".yml";
        FileConfiguration playerConfig = plugin.configManager.get(filePath);

        List<String> commands;
        if (playerConfig.isList("login-task-queue")) {
            commands = playerConfig.getStringList("login-task-queue");
        } else {
            commands = new ArrayList<>();
        }
        commands.add(command);
        playerConfig.set("login-task-queue", commands);

        if (!player.isOnline()) {
            plugin.configManager.unload(filePath, true);
        } else {
            plugin.configManager.save(filePath);
        }
    }

    /**
     * 检查玩家的权限，如果玩家是新人则通知其去做问卷；如果已在验证新人名单里就直接送入玩家组
     * @param player 待检测的玩家实例
     * @throws Exception 指令异常
     */
    public void joinCheck(Player player) throws Exception {
        FileConfiguration newbiesList = plugin.configManager.get("newbies_list.yml");
        boolean need_refresh = false;
        // 获取已通过的新人组的List
        List<String> list_yet = newbiesList.getStringList("yet-passed-newbies");
        // 如果是新人组的话
        if (workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).getGroup().equals(newbiesGroup)) {
            if (list_yet.contains(player.getName())) {
                // 查看玩家是否在已通过新人组，将玩家移入玩家权限组，并更新
                workWorldsPermissionHandler.getUser(player.getUniqueId().toString()).setGroup(playersGroup);
                list_yet.remove(player.getName());
                newbiesList.set("yet-passed-newbies", list_yet);
                need_refresh = true;
            } else {
                plugin.messageManager.sendPlayerMessage(player, plugin.wolfyLanguageAPI.
                        replaceColoredKeys("$module_message.player_manager.you_are_newbie$"));
                player.sendTitle(
                        plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.player_manager.welcome_title$"),
                        plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.player_manager.welcome_subtitle$"),
                        3, 97, 5);
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
            plugin.configManager.save("newbies_list.yml");
        }
    }

    public void showUpdateLog(Player player) throws Exception {
        final String header = plugin.wolfyLanguageAPI.replaceColoredKeys(
                "$module_message.player_manager.updatelog_head$") + "§r\n";
        final String title = plugin.wolfyLanguageAPI.
                replaceColoredKeys("$module_message.player_manager.updatelog_title$");

        FileConfiguration updateLog = plugin.configManager.reload("update_log.yml");
        FileConfiguration playerConfig =
                plugin.configManager.get("player/" + player.getUniqueId().toString() + ".yml");
        long lastTime = playerConfig.getLong("last-login-time", 0);

        List<String> logs = new ArrayList<>();
        logs.add(header);

        ConfigurationSection logsSection = updateLog.getConfigurationSection("logs");
        if (logsSection == null)
            return;
        logsSection.getKeys(false).forEach(date -> {
            try {
                if (plugin.dateFormatter.parse(date).getTime() > lastTime) {
                    logs.add(logsSection.getString(date));
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
        playerConfig.set("last-login-time", System.currentTimeMillis());
        plugin.configManager.save("player/" + player.getUniqueId().toString() + ".yml");
    }

    /**
     * 为某玩家打开某封信
     * @param player 玩家实例
     * @param email 信件名称
     * @return 如果true说明玩家完成阅读，false说明需要回滚到未读状态
     */
    public boolean showEmail(Player player, String email, boolean hasRead) {
        FileConfiguration emailConfig = plugin.configManager.get("email/" + email + ".yml");
        String author = emailConfig.getString("author");
        String title = emailConfig.getString("title");
        String dateString = emailConfig.getString("date");
        String availableDateString = emailConfig.getString("available-until");
        String permission = emailConfig.getString("permission", "");
        String permissionMessage = emailConfig.getString("permission-message", "");
        boolean requireInventory = emailConfig.getBoolean("require-inventory");
        List<String> commands = emailConfig.getStringList("commands");
        long availableTime;
        if (availableDateString != null) {
            try {
                availableTime = plugin.dateFormatter.parse(availableDateString).getTime();
            } catch (Exception e) {
                availableTime = 0;
            }
        } else {
            availableTime = 0;
        }
        //
        List<String> texts = new ArrayList<>();
        texts.add("§l" + title + "§r");
        texts.add("§4" + author + "§r");
        texts.add("§7" + dateString + "§r\n");

        if (commands.size() != 0 && !hasRead) {
            if (availableTime > System.currentTimeMillis()) {
                texts.add(plugin.wolfyLanguageAPI.replaceColoredKeys(
                        "$module_message.player_manager.email_outdated$") + "§r\n");
            } else {
                if (requireInventory && player.getInventory().firstEmpty() == -1) {
                    // 命令需要玩家物品栏有空位置,否则退回
                    texts.add(plugin.wolfyLanguageAPI.replaceColoredKeys
                            ("$module_message.player_manager.email_require_inventory$"));

                    player.openBook(((city.newnan.newnanplus.powertools.PowerTools)
                            plugin.getModule(city.newnan.newnanplus.powertools.PowerTools.class))
                            .createBook(title, author, null, BookMeta.Generation.ORIGINAL, texts));
                    return false;
                } else {
                    CommandSender sender = plugin.getServer().getConsoleSender();
                    commands.forEach(command -> plugin.getServer().
                            dispatchCommand(sender, command.replaceAll("@p", player.getName())));
                }
            }
        }

        // 权限检查
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            if (permissionMessage != null && !permissionMessage.isEmpty()) {
                texts.add(WolfyUtilities.translateColorCodes(permissionMessage));
            } else {
                texts.add(plugin.wolfyLanguageAPI.
                        replaceColoredKeys("$module_message.player_manager.email_no_permission$"));
            }
            player.openBook(((city.newnan.newnanplus.powertools.PowerTools)
                    plugin.getModule(city.newnan.newnanplus.powertools.PowerTools.class))
                    .createBook(title, author, null, BookMeta.Generation.ORIGINAL, texts));
            return false;
        }
        //
        String text = emailConfig.getString("text");
        assert text != null;
        texts.add(text);
        player.openBook(((city.newnan.newnanplus.powertools.PowerTools)
                plugin.getModule(city.newnan.newnanplus.powertools.PowerTools.class))
                .createBook(title, author, null, BookMeta.Generation.ORIGINAL, texts));
        return true;
    }

    public void touchPlayer(Player player) throws Exception {
        String playerConfigPath = "player/" + player.getUniqueId().toString() + ".yml";
        // 如果玩家配置文件是新被创建的
        if (!plugin.configManager.touchOrCopyTemplate(playerConfigPath, "player/template.yml")) {
            plugin.messageManager.printINFO(MessageFormat.format("New player file added: {0}({1})",
                    player.getName(), player.getUniqueId()));
            FileConfiguration playerConfig = plugin.configManager.get(playerConfigPath);
            playerConfig.set("name", player.getName());
            plugin.configManager.save(playerConfigPath);
        }
    }
}
