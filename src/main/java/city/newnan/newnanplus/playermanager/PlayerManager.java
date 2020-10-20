package city.newnan.newnanplus.playermanager;

import city.newnan.newnanplus.GlobalData;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions.BadUsageException;
import city.newnan.newnanplus.exception.CommandExceptions.PlayerMoreThanOneException;
import city.newnan.newnanplus.exception.CommandExceptions.PlayerNotFountException;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlayerManager implements Listener, NewNanPlusModule {
    /**
     * 持久化访问全局数据
     */
    private final GlobalData globalData;

    private Group newbiesGroup;
    private Group playersGroup;
    private OverloadedWorldHolder workWorldsPermissionHandler;

    /**
     * 构造函数
     * @param globalData 全局实例
     */
    public PlayerManager(GlobalData globalData) throws Exception {
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
        workWorldsPermissionHandler = globalData.groupManager.getWorldsHolder().
                getWorldData(config.getString("module-playermanager.world-group"));
        newbiesGroup = workWorldsPermissionHandler.getGroup(config.getString("module-playermanager.newbies-group"));
        playersGroup = workWorldsPermissionHandler.getGroup(config.getString("module-playermanager.player-group"));

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

    public UUID findOnePlayerUUIDByName(String playerName) throws Exception {
        try {
            return findOnePlayerByName(playerName).getUniqueId();
        } catch (Exception e) {
            if (e instanceof PlayerNotFountException) {
                OfflinePlayer[] players = Arrays.stream(globalData.plugin.getServer().getOfflinePlayers()).
                        filter(offlinePlayer -> Objects.equals(offlinePlayer.getName(), playerName)).toArray(OfflinePlayer[]::new);
                if (players.length > 1) {
                    throw new PlayerMoreThanOneException();
                }
                if (players.length == 0) {
                    throw new PlayerNotFountException();
                }
                return players[0].getUniqueId();
            } else {
                throw e;
            }
        }
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
     * @throws Exception 指令异常
     */
    public void joinCheck(Player player) throws Exception {
        FileConfiguration newbiesList = globalData.configManager.get("newbies_list.yml");
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

    /**
     * 为某玩家打开某封信
     * @param player 玩家实例
     * @param email 信件名称
     * @return 如果true说明玩家完成阅读，false说明需要回滚到未读状态
     */
    public boolean showEmail(Player player, String email) {
        FileConfiguration emailConfig = globalData.configManager.get("email/" + email + ".yml");
        String author = emailConfig.getString("author");
        String title = emailConfig.getString("title");
        String dateString = emailConfig.getString("date");
        String availableDateString = emailConfig.getString("available-until");
        String permission = emailConfig.getString("permission", "");
        boolean requireInventory = emailConfig.getBoolean("require-inventory");
        List<String> commands = emailConfig.getStringList("commands");
        long availableTime;
        if (availableDateString != null) {
            try {
                availableTime = globalData.dateFormatter.parse(availableDateString).getTime();
            } catch (Exception e) {
                availableTime = 0;
            }
        } else {
            availableTime = 0;
        }
        String text = emailConfig.getString("text");
        assert text != null;
        //
        ItemStack mailBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) mailBook.getItemMeta();
        assert bookMeta != null;
        bookMeta.setAuthor(author);
        bookMeta.setTitle(title);
        List<String> pages = new ArrayList<>();
        //
        StringBuilder pageBuffer = new StringBuilder();
        pageBuffer.append("§l").append(title).append("§r\n");
        pageBuffer.append("§4").append(author).append("§r\n");
        pageBuffer.append("§7").append(dateString).append("§r\n\n");
        int line = 4;
        //
        if (commands.size() != 0) {
            if (availableTime > System.currentTimeMillis()) {
                pageBuffer.append("§c").append("[信件过期无法领取奖励]").append("§r\n\n");
                line++;
            } else {
                if (requireInventory && player.getInventory().firstEmpty() == -1) {
                    // 命令需要玩家物品栏有空位置,否则退回
                    pages.add(pageBuffer.toString() + "§c该信件包含奖励物品，\n请保证您的背包中有\n足够的空间再来领取！");
                    bookMeta.setPages(pages);
                    mailBook.setItemMeta(bookMeta);
                    player.openBook(mailBook);
                    return false;
                } else {
                    CommandSender sender = globalData.plugin.getServer().getConsoleSender();
                    commands.forEach(command -> globalData.plugin.getServer().
                            dispatchCommand(sender, command.replaceAll("@s", player.getName())));
                }
            }
        }

        // 权限检查
        assert permission != null;
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            pages.add(pageBuffer.toString() + "§c你没有权限打开信件！");
            bookMeta.setPages(pages);
            mailBook.setItemMeta(bookMeta);
            player.openBook(mailBook);
            return false;
        }

        for (String paragraph : text.split("\n")) {
            line++;
            if (paragraph.equals("---"))
                pageBuffer.append("===================").append('\n');
            else
                pageBuffer.append(paragraph).append('\n');
            if (line == 14) {
                pages.add(pageBuffer.toString());
                pageBuffer.delete(0, pageBuffer.length());
                line = 0;
            }
        }
        if (line != 0) {
            pages.add(pageBuffer.toString());
            pageBuffer.delete(0, pageBuffer.length());
        }

        bookMeta.setPages(pages);
        mailBook.setItemMeta(bookMeta);
        player.openBook(mailBook);

        return true;
    }
}
