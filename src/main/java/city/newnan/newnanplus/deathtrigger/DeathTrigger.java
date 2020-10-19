package city.newnan.newnanplus.deathtrigger;

import city.newnan.newnanplus.NewNanPlusGlobal;
import city.newnan.newnanplus.NewNanPlusModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeathTrigger implements Listener, NewNanPlusModule {
    /**
     * 持久化访问全局数据
     */
    private final NewNanPlusGlobal globalData;

    /**
     * 模块设置
     */
    private final ArrayList<CostStage> costStages = new ArrayList<>();

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public DeathTrigger(NewNanPlusGlobal globalData) {
        this.globalData = globalData;
        reloadConfig();
        // 注册监听函数
        this.globalData.plugin.getServer().getPluginManager().registerEvents(this, this.globalData.plugin);
        globalData.deathTrigger = this;
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        // 获取配置实例
        FileConfiguration config = globalData.configManager.get("config.yml");

        // 加载配置内容
        costStages.clear();
        if (config.getBoolean("module-deathcost.use-simple-mode")) {
            // 简单扣费模式
            costStages.add(new CostStage(-1,
                    config.getDouble("module-deathcost.simple-mode.cost"),
                    config.getBoolean("module-deathcost.simple-mode.if-percent")));
        } else {
            // 复杂扣费模式
            List<Map<?,?>> list_map = config.getMapList("module-deathcost.complex-mode");
            for (Map<?,?> map : list_map) {
                costStages.add(new CostStage(
                        (Double) map.get("max"),
                        (Double) map.get("cost"),
                        (Boolean) map.get("if-percent")));
            }
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
    public void onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {

    }

    /**
     * 玩家死亡时触发的方法
     * @param event 玩家死亡事件
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 触发死亡惩罚
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
        double bal = globalData.vaultEco.getBalance(player);
        double cost = 0.0;

        double pre_max = 0.0;
        // 遍历扣费阶梯
        for (CostStage stage : costStages) {
            // 获取阶梯上限
            double max = stage.max;
            if (bal <= pre_max) {
                break;
            }
            // 获取数值
            double value = stage.cost;

            if (stage.ifPercent) {
                value *= (bal - pre_max);
            }
            pre_max = (max == -1) ? 0.0 : max;
            cost += value;
        }

        // 扣钱
        if (cost > 0) {
            globalData.vaultEco.withdrawPlayer(player, cost);
        }

        return cost;
    }

    /**
     * 发送死亡消息
     * @param player 死亡玩家
     * @param cost 玩家死亡花费
     */
    public void sendDeathMessage(Player player, double cost) {
        FileConfiguration config = globalData.configManager.get("config.yml");

        // 向玩家发送消息
        if (config.getBoolean("module-deathcost.msg-player-enable", false)) {
            globalData.sendPlayerMessage(player, MessageFormat.format(globalData.wolfyLanguageAPI.
                            replaceColoredKeys("$module_message.death_trigger.death_message_to_player$")
                            , player.getDisplayName(), cost));
        }
        // 广播发送消息
        if (config.getBoolean("module-deathcost.msg-broadcast-enable", false)) {
            globalData.plugin.getServer().broadcastMessage(MessageFormat.format(globalData.wolfyLanguageAPI.
                            replaceColoredKeys("$module_message.death_trigger.death_message_broadcast$"),
                            player.getDisplayName(), cost));
        }
        // 控制台发送消息
        if (config.getBoolean("module-deathcost.msg-console-enable", false)) {
            globalData.printINFO(MessageFormat.format(globalData.wolfyLanguageAPI.
                            replaceColoredKeys("$module_message.death_trigger.death_message_to_console$"),
                            player.getDisplayName(), cost));
        }
    }
}

class CostStage {
    public double max;
    public double cost;
    public boolean ifPercent;

    public CostStage(double max, double cost, boolean ifPercent) {
        this.max = max;
        this.cost = cost;
        this.ifPercent = ifPercent;
    }
}