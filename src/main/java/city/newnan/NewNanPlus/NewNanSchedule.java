package city.newnan.NewNanPlus;

import java.text.MessageFormat;

import org.bukkit.Sound;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Vector;

public class NewNanSchedule extends BukkitRunnable {
    /**
     * 插件对象，用于持久化存储和访问全局数据
     */
    private NewNanPlusPlugin Plugin = null;
    private Vector<Player> FlyingPlayers = null;
    private Vector<Player> ToDeleteFlyingPlayer = new Vector<Player>();

    public NewNanSchedule(NewNanPlusPlugin plugin) {
        Plugin = plugin;
        FlyingPlayers = plugin.FlyingPlayers;
    }

    @Override
    public void run() {
        if (FlyingPlayers.size() > 0) {
            double cost_per_count = Plugin.getConf().getDouble("module-flyfee.cost-per-count");
            double tick_per_count = Plugin.getConf().getDouble("module-flyfee.tick-per-count");
            double cost_per_second = (20.0 / tick_per_count) * cost_per_count;
            for (Player player : FlyingPlayers) {
                // 获取玩家现金金额
                double balance = Plugin.getVaultEco().getBalance(player);
                // 如果玩家还有现金
                if (balance > 0.0) {
                    int remain_second = (int)(balance / cost_per_second);
                    Plugin.getVaultEco().withdrawPlayer(player, cost_per_count);
                    NewNanPlusPlugin.sendPlayerActionBar(player, MessageFormat.format(
                                    Plugin.getConf().getString("module-flyfee.msg-actionbar"),
                                    formatSecond(remain_second), balance));


                    // 如果只能飞一分钟以内，就警告
                    if (remain_second <= 60.0) {
                        String _msg = ChatColor.translateAlternateColorCodes('&',
                                Plugin.getConf().getString("module-flyfee.msg-feewraning"));
                        player.sendTitle(_msg, null, 1, 7, 2);
                    }
                } else {
                    // 如果玩家没钱了，就踢出去
                    // 不能直接从里面删除，不太好，会让迭代器受损，所以先登记，for完了再删
                    ToDeleteFlyingPlayer.add(player);
                    player.setAllowFlight(false);
                    player.setFlySpeed(1.0f);
                    player.sendMessage(Plugin.getConf().getString("module-flyfee.msg-finish"));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.0f);
                }
            }
            // 删掉刚才需要踢除的
            for (Player player : ToDeleteFlyingPlayer) {
                FlyingPlayers.remove(player);
            }
            ToDeleteFlyingPlayer.clear();
        }
    }

    private static String formatSecond(int second) {
        if (second < 60) {
            return second + "秒";
        }else if (second >= 60 && second < 3600) {
            int m = second / 60;
            int s = second % 60;
            return m + "分" + s + "秒";
        }else if (second >= 3600 && second < 86400) {
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
