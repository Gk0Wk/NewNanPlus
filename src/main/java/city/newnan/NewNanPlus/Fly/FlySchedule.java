package city.newnan.NewNanPlus.Fly;
import city.newnan.NewNanPlus.*;

import java.util.Vector;
import java.text.MessageFormat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FlySchedule extends BukkitRunnable {
    /**
     * 持久化访问全局数据
     */
    private final NewNanPlusGlobal GlobalData;

    public FlySchedule(NewNanPlusGlobal globalData) {
        GlobalData = globalData;
        runTaskTimer(GlobalData.Plugin, 0, GlobalData.Config.getInt("module-flyfee.tick-per-count"));
    }

    @Override
    public void run() {
        if (GlobalData.FlyingPlayers.size() > 0) {
            double cost_per_count = GlobalData.Config.getDouble("module-flyfee.cost-per-count");
            double tick_per_count = GlobalData.Config.getDouble("module-flyfee.tick-per-count");
            double cost_per_second = (20.0 / tick_per_count) * cost_per_count;

            // 不能在遍历的时候删除元组，所以需要暂时记录
            Vector<Player> ToDeleteFlyingPlayer = new Vector<Player>();

            // 遍历飞行玩家 - 改用Lambda forEach
            GlobalData.FlyingPlayers.forEach(((player, flyingPlayer) -> {
                if (player.hasPermission("newnanplus.fly.free")) {
                    GlobalData.sendPlayerActionBar(player, MessageFormat.format(
                            GlobalData.Config.getString("module-flyfee.msg-actionbar-bypass"),
                            player.getName()));
                    // lambda表达式中要用return跳过本次调用，相当于for的continue
                    return;
                }
                // 获取玩家现金金额
                double balance = GlobalData.VaultEco.getBalance(player);
                // 如果玩家还有现金
                if (balance > 0.0) {
                    int remain_second = (int)(balance / cost_per_second);
                    GlobalData.VaultEco.withdrawPlayer(player, (balance < cost_per_count) ? balance : cost_per_count);
                    GlobalData.sendPlayerActionBar(player, MessageFormat.format(
                            GlobalData.Config.getString("module-flyfee.msg-actionbar"),
                            formatSecond(remain_second), balance));

                    // 如果只能飞一分钟以内，就警告
                    if (remain_second <= 60.0) {
                        String _msg = ChatColor.translateAlternateColorCodes('&',
                                GlobalData.Config.getString("module-flyfee.msg-feewraning"));
                        player.sendTitle(_msg, null, 1, 7, 2);
                    }
                } else {
                    // 如果玩家没钱了，就踢出去
                    // 不能直接从里面删除，不太好，会让迭代器受损，所以先登记，for完了再删
                    ToDeleteFlyingPlayer.add(player);
                }
            }));

            // 删掉刚才需要踢除的
            ToDeleteFlyingPlayer.forEach((player) -> {
                GlobalData.FlyCommand.cancelFly(player, true);
            });
            ToDeleteFlyingPlayer.clear();
        }
    }

    private static String formatSecond(int second) {
        if (second < 60) {
            return second + "秒";
        }else if (second < 3600) {
            int m = second / 60;
            int s = second % 60;
            return m + "分" + s + "秒";
        }else if (second < 86400) {
            int h = second / 3600;
            int m = (second % 3600) / 60;
            int s = (second % 3600) % 60;
            return h + "小时" + m + "分" + s + "秒";
        } else {
            int d = (second / 3600) / 24;
            int h = (second / 3600) % 24;
            int m = (second % 3600) / 60;
            int s = (second % 3600) % 60;
            return d + "天" + h + "小时" + m + "分" + s + "秒";
        }
    }
}