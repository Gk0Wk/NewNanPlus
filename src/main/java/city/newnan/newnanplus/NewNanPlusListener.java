package city.newnan.newnanplus;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

/**
 * 插件监听类，用于一些全局事件的监听。
 * 各模块的监听实现于各模块内。
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
