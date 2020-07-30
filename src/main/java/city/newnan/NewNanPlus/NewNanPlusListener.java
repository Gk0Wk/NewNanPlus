package city.newnan.NewNanPlus;

import java.text.MessageFormat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class NewNanPlusListener implements Listener {
    /**
     * 插件对象，用于持久化存储和访问全局数据
     */
    private NewNanPlusPlugin Plugin;

    public NewNanPlusListener(NewNanPlusPlugin plugin) {
        Plugin = plugin;
        
        plugin.getServer().getPluginManager().registerEvents(this, Plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 获得玩家对象
        Player player = event.getEntity();
        
        // 如果玩家没有死亡不扣钱权限
        if (!player.hasPermission("newnanplus.deathcost.pypass")) {
            // 获取相关设置 - 每一次获取一次，这样在运行中重载配置才有效
            double DeathCost_Min = Plugin.getConf().getDouble("module-deathcost.min");
            double DeathCost_Max = Plugin.getConf().getDouble("module-deathcost.max");
            double DeathCost_Rate = Plugin.getConf().getDouble("module-deathcost.rate");

            // 获得玩家的现金
            double bal = Plugin.getVaultEco().getBalance(player);
            double cost = 0.0;
            if (bal >= DeathCost_Min) {
                if (bal <= DeathCost_Max) {
                    cost = bal * DeathCost_Rate;
                } else {
                    cost = DeathCost_Max * DeathCost_Rate;
                }
                // 扣钱
                Plugin.getVaultEco().withdrawPlayer(player, cost);
            }

            // 向玩家发送消息
            if (Plugin.getConf().getBoolean("module-deathcost.msg-player.enable")) {
                Plugin.sendPlayerMessage(player,
                        MessageFormat.format(
                                Plugin.getConf().getString("module-deathcost.msg-player.text"),
                                player.getDisplayName(), cost
                        ));
            }
            // 广播发送消息
            if (Plugin.getConf().getBoolean("module-deathcost.msg-broadcast.enable")) {
                String msg = ChatColor.translateAlternateColorCodes('&',
                        Plugin.getConf().getString("module-deathcost.msg-player.text"));
                Plugin.getServer().broadcastMessage(MessageFormat.format(msg,
                        player.getDisplayName(), cost));
            }
            // 控制台发送消息
            if (Plugin.getConf().getBoolean("module-deathcost.msg-console.enable")) {
                String msg = ChatColor.translateAlternateColorCodes('&',
                        Plugin.getConf().getString("module-deathcost.msg-console.text"));
                Plugin.printINFO(MessageFormat.format(msg, player.getDisplayName(), cost));
            }
        }

        if (Plugin.FlyingPlayers.contains(player)) {
            Plugin.cancelFly(player, false);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        // 如果这个玩家之前在飞
        if (Plugin.FlyingPlayers.contains(event.getPlayer())) {
            Player player = event.getPlayer();
            // 如果传送后不在一个世界
            if (event.getFrom().getWorld() != event.getTo().getWorld()) {
                Plugin.cancelFly(event.getPlayer(), true);
                player.sendMessage(Plugin.getConf().getString("module-flyfee.msg-finish"));
            }
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (Plugin.FlyingPlayers.contains(event.getPlayer())) {
            Plugin.cancelFly(event.getPlayer(), false);
        }
    }
}
