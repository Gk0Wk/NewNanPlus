package city.newnan.NewNanPlus.DeathTrigger;

import city.newnan.NewNanPlus.NewNanPlusGlobal;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

public class DTCommand {
    /**
     * 持久化访问全局数据
     */
    private final NewNanPlusGlobal GlobalData;

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public DTCommand(NewNanPlusGlobal globalData) {
        GlobalData = globalData;
    }

    /**
     * 玩家死亡时触发的方法
     * @param event 玩家死亡事件
     */
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        double cost = fineMoney(player);
        sendDeathMessage(player, cost);
    }

    /**
     * 死亡罚款
     * @param player 死亡的玩家
     * @return 扣的款数(调用方法就已经扣款，不用再扣一次)
     */
    public double fineMoney(Player player) {
        // 如果玩家有死亡不扣钱权限
        if (player.hasPermission("newnanplus.deathcost.bypass")) {
            return 0.0;
        }

        // 获得玩家的现金
        double bal = GlobalData.VaultEco.getBalance(player);
        double cost = 0.0;

        if (GlobalData.Config.getBoolean("module-deathcost.use-simple-mode")) {
            // 简单扣费模式
            cost = GlobalData.Config.getDouble("module-deathcost.simple-mode.cost");
            // 如果是百分比模式
            if (GlobalData.Config.getBoolean("module-deathcost.simple-mode.if-percent")) {
                cost *= bal;
            }
        } else {
            // 复杂扣费模式
            List<Map<?,?>> list_map = GlobalData.Config.getMapList("module-deathcost.complex-mode");

            double pre_max = 0.0;
            // 遍历扣费阶梯
            for (Map<?,?> map : list_map) {
                // 获取阶梯上限
                double max = (Double) map.get("max");
                if (bal <= pre_max) {
                    break;
                }
                // 获取数值
                double value = (Double) map.get("cost");

                if ((Boolean) map.get("if-percent")) {
                    value *= (bal - pre_max);
                }
                pre_max = (max == -1) ? 0.0 : max;
                cost += value;
            }
        }

        // 扣钱
        if (cost > 0) {
            GlobalData.VaultEco.withdrawPlayer(player, cost);
        }

        return cost;
    }

    /**
     * 发送死亡消息
     * @param player 死亡玩家
     * @param cost 玩家死亡花费
     */
    public void sendDeathMessage(Player player, double cost) {
        // 向玩家发送消息
        if (GlobalData.Config.getBoolean("module-deathcost.msg-player.enable")) {
            GlobalData.sendPlayerMessage(player,
                    MessageFormat.format(
                            GlobalData.Config.getString("module-deathcost.msg-player.text"),
                            player.getDisplayName(), cost
                    ));
        }
        // 广播发送消息
        if (GlobalData.Config.getBoolean("module-deathcost.msg-broadcast.enable")) {
            String msg = ChatColor.translateAlternateColorCodes('&',
                    GlobalData.Config.getString("module-deathcost.msg-player.text"));
            GlobalData.Plugin.getServer().broadcastMessage(MessageFormat.format(msg,
                    player.getDisplayName(), cost));
        }
        // 控制台发送消息
        if (GlobalData.Config.getBoolean("module-deathcost.msg-console.enable")) {
            String msg = ChatColor.translateAlternateColorCodes('&',
                    GlobalData.Config.getString("module-deathcost.msg-console.text"));
            GlobalData.printINFO(MessageFormat.format(msg, player.getDisplayName(), cost));
        }
    }
}
