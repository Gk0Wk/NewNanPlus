package city.newnan.newnanplus.playermanager;

import city.newnan.newnanplus.NewNanPlusGlobal;
import city.newnan.newnanplus.NewNanPlusModule;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerManager implements Listener, NewNanPlusModule {
    /**
     * 持久化访问全局数据
     */
    private final NewNanPlusGlobal globalData;

    private final HashMap<String, UUID> reversePlayerList = new HashMap<>();

    private String newbiesGroup;
    private String playersGroup;
    private String workWorldsGroup;
    private String allowLaterMessage;
    private String allowSucceedMessage;
    private String notNewbieAlreadyMessage;

    /**
     * 构造函数
     * @param globalData 全局实例
     */
    public PlayerManager(NewNanPlusGlobal globalData) {
        this.globalData = globalData;
        reloadConfig();

        // 创建反射表
        for (OfflinePlayer player : globalData.plugin.getServer().getOfflinePlayers()) {
            reversePlayerList.put(player.getName(), player.getUniqueId());
        }

        // 注册监听函数
        this.globalData.plugin.getServer().getPluginManager().registerEvents(this, this.globalData.plugin);
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
        allowLaterMessage = config.getString("module-playermanager.msg-allow-when-online");
        allowSucceedMessage = config.getString("module-playermanager.msg-allow-succeed");
        notNewbieAlreadyMessage = config.getString("module-playermanager.msg-not-newbie-already");
    }

    /**
     * 玩家登录时触发的方法
     * @param event 玩家登录事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        touchPlayer(event.getPlayer());
        joinCheck(event.getPlayer());
    }

    /**
     * /nnp allow指令的实现，将一个玩家纳入已验证新人名单中，如果玩家已经在线，那么就直接赋予玩家权限
     * @param sender 命令的发送者
     * @param args 命令的参数，包括allow
     * @return
     */
    public boolean allowNewbieToPlayer(CommandSender sender, String args[]) {
        // 检查权限
        if (!sender.hasPermission("nnp.newbies.allow")) {
            globalData.sendMessage(sender, globalData.globalMessage.get("NO_PERMISSION"));
        }

        // 寻找目标玩家
        Player player = globalData.plugin.getServer().getPlayer(args[1]);
        // 是否需要刷新新人名单
        boolean need_refresh = false;

        FileConfiguration newbiesList = globalData.configManager.get("newbies_list.yml");

        // 如果玩家不在线或不存在，就存到配置里
        if (player == null) {
            // 获取未通过的新人组的List
            List<String> list_not = (List<String>) newbiesList.getList("not-passed-newbies");
            // 如果未通过里有这个玩家，那就去掉
            assert list_not != null;
            if (list_not.contains(args[1])) {
                list_not.remove(args[1]);
                newbiesList.set("not-passed-newbies", list_not);
                need_refresh = true;
            }
            // 获取已通过的新人组的List
            List<String> list_yet = (List<String>) newbiesList.getList("yet-passed-newbies");
            // 如果通过里没有这个玩家，就加入
            assert list_yet != null;
            if (!list_yet.contains(args[1])) {
                list_yet.add(args[1]);
                newbiesList.set("yet-passed-newbies", list_yet);
                need_refresh = true;
            }
            globalData.sendMessage(sender, allowLaterMessage);
        } else {
            // 如果玩家在线，就直接赋予权限
            // 但是要检查一下原来所在的组
            if (globalData.vaultPerm.getPrimaryGroup(player).equalsIgnoreCase(newbiesGroup)) {
                globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                        "manuadd " + player.getName() + " " + playersGroup + " " + workWorldsGroup);
                // 获取未通过的新人组的List
                List<String> list_not = (List<String>) newbiesList.getList("not-passed-newbies");
                // 如果未通过里有这个玩家，那就去掉
                assert list_not != null;
                if (list_not.contains(player.getName())) {
                    list_not.remove(player.getName());
                    newbiesList.set("not-passed-newbies", list_not);
                    need_refresh = true;
                }
                globalData.sendMessage(sender, allowSucceedMessage);
            } else {
                globalData.sendMessage(sender, notNewbieAlreadyMessage);
            }
        }
        if (need_refresh) {
            globalData.configManager.save("newbies_list.yml");
        }
        return true;
    }

    /**
     * 检查玩家的权限，如果玩家是新人则通知其去做问卷；如果已在验证新人名单里就直接送入玩家组
     * @param player 待检测的玩家实例
     */
    public void joinCheck(Player player) {
        FileConfiguration newbiesList = globalData.configManager.get("newbies_list.yml");
        boolean need_refresh = false;
        // 获取已通过的新人组的List
        List<String> list_yet = (List<String>) newbiesList.getList("yet-passed-newbies");
        // 如果是新人组的话
        if (globalData.vaultPerm.getPrimaryGroup(player).equalsIgnoreCase(newbiesGroup)) {
            assert list_yet != null;
            if (list_yet.contains(player.getName())) {
                // 查看玩家是否在已通过新人组，将玩家移入玩家权限组，并更新
                globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                        "manuadd " + player.getName() + " " + playersGroup + " " + workWorldsGroup);
                list_yet.remove(player.getName());
                newbiesList.set("yet-passed-newbies", list_yet);
                need_refresh = true;
            } else {
                globalData.sendPlayerMessage(player, "&c你还没有获得游玩权限，请在官网完成新人试卷后向管理索要权限。");
                // 获取未通过的新人组的List
                List<String> list_not = (List<String>) newbiesList.getList("not-passed-newbies");
                assert list_not != null;
                if (!list_not.contains(player.getName())) {
                    // 查看玩家是否在未通过新人组，没加入就加入，并更新
                    list_not.add(player.getName());
                    newbiesList.set("not-passed-newbies", list_not);
                    need_refresh = true;
                }
            }
        } else {
            assert list_yet != null;
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
     * 检测这个玩家是否不在反射表中(是否第一次加入游戏)，并将其加入反射表
     * @param player 玩家实例
     * @return 如果玩家已经在反射表中则返回true，反之
     */
    public boolean touchPlayer(Player player) {
        if (reversePlayerList.containsKey(player.getName()))
            return true;
        reversePlayerList.put(player.getName(), player.getUniqueId());
        return false;
    }

    /**
     * 通过玩家名称反查玩家UUID
     * @param playerName 玩家名称
     * @return 玩家UUID，找不到则返回null
     */
    public UUID findUserWithName(String playerName) {
        return reversePlayerList.get(playerName);
    }
}
