package city.newnan.newnanplus.deathtrigger;

import city.newnan.api.config.ConfigManager;
import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import me.lucko.helper.config.ConfigurationNode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;

public class DeathTrigger implements Listener, NewNanPlusModule {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    /**
     * 模块设置
     */
    private final ArrayList<CostStage> costStages = new ArrayList<>();

    /**
     * 构造函数
     */
    public DeathTrigger() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManagers.get("config.yml").getNode("module-deathtrigger", "enable").getBoolean(false)) {
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
        costStages.clear();

        try {
            // 加载配置内容
            ConfigurationNode costConfig = plugin.configManagers.get("config.yml").getNode("module-deathtrigger", "cash-cost");
            if (costConfig.getNode("use-simple-mode").getBoolean()) {
                // 简单扣费模式
                costStages.add(new CostStage(-1,
                        costConfig.getNode("simple-mode", "cost").getDouble(),
                        costConfig.getNode("simple-mode", "if-percent").getBoolean()));
            } else {
                // 复杂扣费模式
                costConfig.getNode("complex-mode").getChildrenList().forEach(stageNode -> costStages.add(new CostStage(
                        stageNode.getNode("max").getDouble(),
                        stageNode.getNode("cost").getDouble(),
                        stageNode.getNode("if-percent").getBoolean()
                )));
            }
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
        double bal = plugin.vaultEco.getBalance(player);
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
            plugin.vaultEco.withdrawPlayer(player, cost);
        }

        return cost;
    }

    /**
     * 发送死亡消息
     * @param player 死亡玩家
     * @param cost 玩家死亡花费
     */
    public void sendDeathMessage(Player player, double cost) {
        try {
            ConfigurationNode config = plugin.configManagers.get("config.yml").getNode("module-deathtrigger", "death-message");

            // 向玩家发送消息
            if (config.getNode("player-enable").getBoolean(false)) {
                plugin.messageManager.printf(player, "$module_message.death_trigger.death_message_to_player$", player.getDisplayName(), cost);
            }
            // 广播发送消息
            if (config.getNode("broadcast-enable").getBoolean(false)) {
                plugin.getServer().broadcastMessage(plugin.messageManager.sprintf(
                        "$module_message.death_trigger.death_message_broadcast$", player.getDisplayName(), cost));
            }
            // 控制台发送消息
            if (config.getNode("console-enable").getBoolean(false)) {
                plugin.messageManager.printf("$module_message.death_trigger.death_message_to_console$", player.getDisplayName(), cost);
            }
        } catch (IOException | ConfigManager.UnknownConfigFileFormatException e) {
            e.printStackTrace();
        }
    }

    static class CostStage {
        public double max;
        public double cost;
        public boolean ifPercent;

        public CostStage(double max, double cost, boolean ifPercent) {
            this.max = max;
            this.cost = cost;
            this.ifPercent = ifPercent;
        }
    }
}

