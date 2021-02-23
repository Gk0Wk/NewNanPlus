package city.newnan.newnanplus.dynamiceconomy;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.utility.ItemKit;
import java.util.ArrayList;
import java.util.Objects;
import me.lucko.helper.config.ConfigurationNode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.quickshop.shop.ContainerShop;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.ShopType;

class SystemCommodity {
    public String name;
    public ItemStack itemStack;
    public int amount;
    public double value;
    public double sellResponseVolume, buyResponseVolume;
    public long lastSellTime, lastBuyTime;
    public double buyValue, sellValue;
    public ArrayList<Shop> shopList = new ArrayList<>();

    public static OfflinePlayer ntOwner;
    public static DynamicEconomy dynamicEconomy;
    public static NewNanPlus plugin;

    public SystemCommodity(ConfigurationNode commodityNode)
    {
        name = (String)commodityNode.getKey();
        String data = commodityNode.getNode("data").getString();
        assert data != null;
        // 两种不同的构造方式。{开头的是JSON格式，反之是base64格式
        if (data.charAt(0) == '{') {
            itemStack = ItemKit.convertJsontoItemStack(data);
        } else {
            itemStack = ItemKit.deserializeNMSItemStack(data);
        }

        amount = commodityNode.getNode("amount").getInt(0);
        value = commodityNode.getNode("value").getDouble(0.0);
        sellResponseVolume = commodityNode.getNode("sell-response-volume").getDouble(0.0);
        buyResponseVolume = commodityNode.getNode("buy-response-volume").getDouble(0.0);
        lastSellTime = commodityNode.getNode("last-sell-time").getLong(0L);
        lastBuyTime = commodityNode.getNode("last-buy-time").getLong(0L);

        updatePrice();
    }

    /**
     * 收购：官方 <- 玩家
     *
     * @param amount 收购数量
     */
    public void buy(long amount)
    {
        // 维持国库所有者的虚拟存款在500000以上
        if (plugin.vaultEco.getBalance(ntOwner) < 500000.0)
            plugin.vaultEco.depositPlayer(ntOwner, 500000.0 - plugin.vaultEco.getBalance(ntOwner));

        this.amount += amount;

        long curTime = System.currentTimeMillis();
        // 计算γ
        double gamma = (lastBuyTime == 0) ? 0.0 : (10.0 / (10.0 + Math.log10(1 + curTime - lastBuyTime)));

        // 更新时间
        lastBuyTime = curTime;

        // 更新响应量
        buyResponseVolume = amount + gamma * buyResponseVolume;

        // 更新商品价值
        updatePrice();

        // 刷新所有商店
        updateShops();
    }

    /**
     * 售卖：官方 -> 玩家
     *
     * @param amount 售卖数量
     */
    public void sell(long amount)
    {
        this.amount -= amount;

        // 小于0检查，虽然一般不可能出现这种情况，但是还是检测一下
        if (this.amount < 0)
            this.amount = 0;

        long curTime = System.currentTimeMillis();
        // 计算γ
        double gamma = (lastSellTime == 0) ? 0.0 : (10.0 / (10.0 + Math.log10(1 + curTime - lastSellTime)));

        // 更新时间
        lastSellTime = curTime;

        // 更新响应量
        sellResponseVolume = amount + gamma * sellResponseVolume;

        // 更新商品价值
        updatePrice();

        // 刷新所有商店
        updateShops();
    }

    final static double EPSILON = 0.001;

    private void updatePrice()
    {
        // 计算响应比
        double ratio = (amount + sellResponseVolume + EPSILON) / (amount + buyResponseVolume + EPSILON);
        if (ratio > 10) {
            ratio = 10;
        } else if (ratio < 1.0) {
            ratio = 1.0;
        }
        buyValue = value * Math.pow(ratio, 0.8);
        sellValue = value * Math.pow(ratio, 1.2);
    }

    public void updateShops()
    {
        shopList.forEach(this::updateShop);
    }

    public void updateShop(Shop shop)
    {
        // 更新库存
        Objects.requireNonNull(((ContainerShop)shop).getInventory()).clear();
        shop.add(itemStack, amount);

        //更新价格
        if (shop.getShopType().equals(ShopType.BUYING)) {
            // 收购商店
            shop.setPrice(dynamicEconomy.buyCurrencyIndex * buyValue);
        } else {
            // 售卖商店
            shop.setPrice(dynamicEconomy.sellCurrencyIndex * sellValue);
        }
    }

    public void saveCommodityToSection()
    {
        ConfigurationNode section = dynamicEconomy.commodities.getNode(name);

        Material type = itemStack.getType();
        // 带文字的书、潜影盒属于json化内容很多的，转化为base64存储
        if (type.equals(Material.WRITABLE_BOOK) || type.equals(Material.WRITTEN_BOOK) || type.equals(Material.SHULKER_BOX)) {
            section.getNode("data").setValue(ItemKit.serializeNMSItemStack(itemStack));
        } else {
            section.getNode("data").setValue(ItemKit.convertItemStackToJson(itemStack));
        }

        section.getNode("amount").setValue(amount);
        section.getNode("value").setValue(value);
        section.getNode("sell-response-volume").setValue(sellResponseVolume);
        section.getNode("last-sell-time").setValue(lastSellTime);
        section.getNode("buy-response-volume").setValue(buyResponseVolume);
        section.getNode("last-buy-time").setValue(lastBuyTime);

        // dynamicEconomy.commodities.getNode(name).setValue(section);
    }
}
