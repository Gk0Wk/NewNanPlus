package city.newnan.NewNanPlus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.*;
import org.bukkit.inventory.Inventory;

import org.maxgamer.quickshop.event.ShopSuccessPurchaseEvent;
import org.maxgamer.quickshop.shop.ShopType;

public class NewNanPlusListener implements Listener {
    /**
     * 插件对象，用于持久化存储和访问全局数据
     */
    private final NewNanPlusGlobal GlobalData;

    /**
     * 初始化实例
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public NewNanPlusListener(NewNanPlusGlobal globalData) {
        GlobalData = globalData;
        GlobalData.Plugin.getServer().getPluginManager().registerEvents(this, GlobalData.Plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 获得玩家对象
        Player player = event.getEntity();

        // 别飞了，给我下来
        GlobalData.FlyCommand.cancelFly(player, false);

        GlobalData.DTCommand.onDeath(event);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        // 如果传送后不在一个世界
        if (event.getFrom().getWorld() != event.getTo().getWorld()) {
            GlobalData.FlyCommand.cancelFly(event.getPlayer(), true);
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        GlobalData.PlayerCommand.joinCheck(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 取消飞行
        GlobalData.FlyCommand.cancelFly(event.getPlayer(), false);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer().hasPermission("newnanplus.invcheck.bypass")) {
            return;
        }

        GlobalData.printINFO(event.getPlayer().getName() + " open the inventory.");

        Inventory inv = event.getInventory();
        if (inv.contains(Material.BARRIER)) {
            GlobalData.printINFO(event.getPlayer().getName() + " has Barrier!");
        }
        if (inv.contains(Material.BEDROCK)) {
            GlobalData.printINFO(event.getPlayer().getName() + " has Bedrock!");
        }
    }

    @EventHandler
    public void onServerLoadEvent(ServerLoadEvent event) {
        GlobalData.CornCommand.runOnServerReady();
    }

    @EventHandler(ignoreCancelled = true)
    public void shopPurchase(ShopSuccessPurchaseEvent event) {
        // event.getShop().getShopType()    ShopType.BUYING       ShopType.SELLING
        // event.getShop()
        // event.getPlayer()
    }

    // @EventHandler
    public void onInventoryMoveItem_Abandoned(InventoryMoveItemEvent e) {
        GlobalData.LACommand.onInventoryMoveItem(e);
    }
}
