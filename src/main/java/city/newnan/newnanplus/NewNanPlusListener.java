package city.newnan.newnanplus;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

/**
 * 插件负责全局事件监听的类。插件当前采用的逻辑是，全局监听类捕捉事件，不同事件再去分别调用各个模块内的一些方法。
 * 这种方式不适合可拆卸模块化设计，未来可能会被替换成每个模块各自的监听类。
 */
public class NewNanPlusListener implements Listener {
    /**
     * 插件对象，用于持久化存储和访问全局数据
     */
    private final NewNanPlusGlobal globalData;

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public NewNanPlusListener(NewNanPlusGlobal globalData) {
        this.globalData = globalData;
        this.globalData.plugin.getServer().getPluginManager().registerEvents(this, this.globalData.plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 获得玩家对象
        Player player = event.getEntity();

        // 别飞了，给我下来
        globalData.flyCommand.cancelFly(player, false);

        // 触发死亡惩罚
        globalData.dtCommand.onDeath(event);
    }

//    @EventHandler
//    public void onTeleport(PlayerTeleportEvent event) {
//    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // 如果新的世界没有该权限，就取消玩家的飞行
        if (!event.getPlayer().hasPermission("newnanplus.fly.self"))
            globalData.flyCommand.cancelFly(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        // 如果玩家切换成创造或者旁观者模式，就取消玩家的飞行
        if (event.getNewGameMode().equals(GameMode.CREATIVE) || event.getNewGameMode().equals(GameMode.SPECTATOR))
            globalData.flyCommand.cancelFly(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        globalData.playerCommand.touchPlayer(event.getPlayer());
        globalData.playerCommand.joinCheck(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 取消飞行
        globalData.flyCommand.cancelFly(event.getPlayer(), false);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer().hasPermission("newnanplus.invcheck.bypass")) {
            return;
        }

        globalData.printINFO(event.getPlayer().getName() + " open the inventory.");

        Inventory inv = event.getInventory();
        if (inv.contains(Material.BARRIER)) {
            globalData.printINFO(event.getPlayer().getName() + " has Barrier!");
        }
        if (inv.contains(Material.BEDROCK)) {
            globalData.printINFO(event.getPlayer().getName() + " has Bedrock!");
        }
    }

//    @EventHandler(ignoreCancelled = true)
//    public void shopPurchase(ShopSuccessPurchaseEvent event) {
        // event.getShop().getShopType()    ShopType.BUYING       ShopType.SELLING
        // event.getShop()
        // event.getPlayer()
//    }

//    @EventHandler
//    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
//        GlobalData.LACommand.onInventoryMoveItem(e);
//    }
}
