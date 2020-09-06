package city.newnan.NewNanPlus.Player;

import city.newnan.NewNanPlus.NewNanPlusGlobal;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerCommand {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal GlobalData;

    /**
     * 构造函数
     * @param globalData 全局实例
     */
    public PlayerCommand(NewNanPlusGlobal globalData) {
        GlobalData = globalData;
        GlobalData.NewbiesList = GlobalData.Plugin.loadConf("newbies_list.yml");

        // 创建反射表
        for (OfflinePlayer player : GlobalData.Plugin.getServer().getOfflinePlayers()) {
            GlobalData.ReversePlayerList.put(player.getName(), player.getUniqueId());
        }
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
            GlobalData.sendMessage(sender, GlobalData.Config.getString("global-data.no-permission-msg"));
        }

        // 寻找目标玩家
        Player player = GlobalData.Plugin.getServer().getPlayer(args[1]);
        // 是否需要刷新新人名单
        boolean need_refresh = false;

        // 如果玩家不在线或不存在，就存到配置里
        if (player == null) {
            // 获取未通过的新人组的List
            List<String> list_not = (List<String>) GlobalData.NewbiesList.getList("not-passed-newbies");
            // 如果未通过里有这个玩家，那就去掉
            if (list_not.contains(args[1])) {
                list_not.remove(args[1]);
                GlobalData.NewbiesList.set("not-passed-newbies", list_not);
                need_refresh = true;
            }
            // 获取已通过的新人组的List
            List<String> list_yet = (List<String>) GlobalData.NewbiesList.getList("yet-passed-newbies");
            // 如果通过里没有这个玩家，就加入
            if (!list_yet.contains(args[1])) {
                list_yet.add(args[1]);
                GlobalData.NewbiesList.set("yet-passed-newbies", list_yet);
                need_refresh = true;
            }
            GlobalData.sendMessage(sender, "操作成功，玩家未上线，稍后玩家登录后将自动获得权限。");
        } else {
            // 如果玩家在线，就直接赋予权限
            // 但是要检查一下原来所在的组
            if (GlobalData.VaultPerm.getPrimaryGroup(player).
                    equalsIgnoreCase(GlobalData.Config.getString("module-allownewbies.newbies-group"))) {
                GlobalData.Plugin.getServer().dispatchCommand(GlobalData.Plugin.getServer().getConsoleSender(),
                        "manuadd " + player.getName()
                                + " " + GlobalData.Config.getString("module-allownewbies.player-group")
                                + " " + GlobalData.Config.getString("module-allownewbies.world-group"));
                // 获取未通过的新人组的List
                List<String> list_not = (List<String>) GlobalData.NewbiesList.getList("not-passed-newbies");
                // 如果未通过里有这个玩家，那就去掉
                if (list_not.contains(player.getName())) {
                    list_not.remove(player.getName());
                    GlobalData.NewbiesList.set("not-passed-newbies", list_not);
                    need_refresh = true;
                }
                GlobalData.sendMessage(sender, "操作成功，赋予玩家权限。");
            } else {
                GlobalData.sendMessage(sender, "该玩家已不是新人。");
            }
        }
        if (need_refresh) {
            GlobalData.Plugin.saveConf("newbies_list.yml", GlobalData.NewbiesList);
        }
        return true;
    }

    /**
     * 检查玩家的权限，如果玩家是新人则通知其去做问卷；如果已在验证新人名单里就直接送入玩家组
     * @param player 待检测的玩家实例
     */
    public void joinCheck(Player player) {
        boolean need_refresh = false;
        // 获取已通过的新人组的List
        List<String> list_yet = (List<String>) GlobalData.NewbiesList.getList("yet-passed-newbies");
        // 如果是新人组的话
        if (GlobalData.VaultPerm.getPrimaryGroup(player).
                equalsIgnoreCase(GlobalData.Config.getString("module-allownewbies.newbies-group"))) {
            if (list_yet.contains(player.getName())) {
                // 查看玩家是否在已通过新人组，将玩家移入玩家权限组，并更新
                GlobalData.Plugin.getServer().dispatchCommand(GlobalData.Plugin.getServer().getConsoleSender(),
                        "manuadd " + player.getName()
                                + " " + GlobalData.Config.getString("module-allownewbies.player-group")
                                + " " + GlobalData.Config.getString("module-allownewbies.world-group"));
                list_yet.remove(player.getName());
                GlobalData.NewbiesList.set("yet-passed-newbies", list_yet);
                need_refresh = true;
            } else {
                GlobalData.sendPlayerMessage(player, "&c你还没有获得游玩权限，请在官网完成新人试卷后向管理索要权限。");
                // 获取未通过的新人组的List
                List<String> list_not = (List<String>) GlobalData.NewbiesList.getList("not-passed-newbies");
                if (!list_not.contains(player.getName())) {
                    // 查看玩家是否在未通过新人组，没加入就加入，并更新
                    list_not.add(player.getName());
                    GlobalData.NewbiesList.set("not-passed-newbies", list_not);
                    need_refresh = true;
                }
            }
        } else if (list_yet.contains(player.getName())){
            // 看看是不是已经成为玩家但是还在名单里
            list_yet.remove(player.getName());
            GlobalData.NewbiesList.set("yet-passed-newbies", list_yet);
            need_refresh = true;
        }
        if (need_refresh) {
            GlobalData.Plugin.saveConf("newbies_list.yml", GlobalData.NewbiesList);
        }
    }

    /**
     * 检测这个玩家是否不在反射表中(是否第一次加入游戏)，并将其加入反射表
     * @param player 玩家实例
     * @return 如果玩家已经在反射表中则返回true，反之
     */
    public boolean touchPlayer(Player player) {
        if (GlobalData.ReversePlayerList.containsKey(player.getName()))
            return true;
        GlobalData.ReversePlayerList.put(player.getName(), player.getUniqueId());
        return false;
    }
}
