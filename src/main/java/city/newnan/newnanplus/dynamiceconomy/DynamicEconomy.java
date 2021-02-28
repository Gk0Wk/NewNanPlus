package city.newnan.newnanplus.dynamiceconomy;

import city.newnan.api.config.ConfigManager;
import city.newnan.api.config.ConfigUtil;
import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions;
import city.newnan.newnanplus.exception.ModuleExeptions;
import me.lucko.helper.config.ConfigurationNode;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.event.*;
import org.maxgamer.quickshop.shop.ContainerShop;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.ShopType;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

public class DynamicEconomy implements NewNanPlusModule, Listener {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    private final HashSet<World> excludeWorld = new HashSet<>();

    private final UUID ntOwner;
    private final HashMap<Material, ArrayList<SystemCommodity>> systemCommodityListMap = new HashMap<>();

    private final Scoreboard scoreboard;
    private final HashSet<Player> displayEcoInfoPlayers = new HashSet<>();

    /**
     * 价值资源矿与价值资源的对应
     */
    private static final HashMap<Material, Material> valueResourceBlockItemMap = new HashMap<Material, Material>() {{
        put(Material.COAL_ORE, Material.COAL);             // 煤矿 -> 煤炭
        put(Material.IRON_ORE, Material.IRON_INGOT);       // 铁矿 -> 铁锭
        put(Material.GOLD_ORE, Material.GOLD_INGOT);       // 金矿 -> 金锭
        put(Material.REDSTONE_ORE, Material.REDSTONE);     // 红石矿 -> 红石
        put(Material.LAPIS_ORE, Material.LAPIS_LAZULI);    // 青金石矿 -> 青金石
        put(Material.DIAMOND_ORE, Material.DIAMOND);       // 钻石矿 -> 钻石
        put(Material.EMERALD_ORE, Material.EMERALD);       // 绿宝石矿 -> 绿宝石
        put(Material.NETHER_QUARTZ_ORE, Material.QUARTZ);  // 下界石英矿 -> 下界石英
    }};

    private static final HashMap<Material, Material> valueResourceItemBlockMap = new HashMap<>();

    /**
     * 价值资源的价值量
     */
    private static final HashMap<Material, Double> valueResourceValueMap = new HashMap<Material, Double>() {{
        put(Material.COAL_ORE, 1.7);          // 煤矿石
        put(Material.IRON_ORE, 3.2);          // 铁矿石
        put(Material.GOLD_ORE, 12.5);         // 金矿石
        put(Material.REDSTONE_ORE, 35.0);     // 红石矿石
        put(Material.LAPIS_ORE, 90.0);        // 青金石矿石
        put(Material.DIAMOND_ORE, 100.0);     // 钻石矿石
        put(Material.EMERALD_ORE, 620.0);     // 绿宝石矿石
        put(Material.NETHER_QUARTZ_ORE, 5.2); // 下界石英矿石
    }};

    /**
     * 价值资源的采集总量
     */
    private final HashMap<Material, Long> valueResourceCounter = new HashMap<Material, Long>() {{
        put(Material.COAL_ORE, 0L);          // 煤矿石
        put(Material.IRON_ORE, 0L);          // 铁矿石
        put(Material.GOLD_ORE, 0L);          // 金矿石
        put(Material.REDSTONE_ORE, 0L);      // 红石矿石
        put(Material.LAPIS_ORE, 0L);         // 青金石矿石
        put(Material.DIAMOND_ORE, 0L);       // 钻石矿石
        put(Material.EMERALD_ORE, 0L);       // 绿宝石矿石
        put(Material.NETHER_QUARTZ_ORE, 0L); // 下界石英矿石
    }};

    public ConfigurationNode commodities;

    /**
     * 系统总价值量
     */
    private double systemTotalWealth;

    /**
     * 货币发行量
     */
    private double currencyIssuance;

    /**
     * 国库货币储量
     */
    private double nationalTreasury;

    /**
     * 参考货币指数，收购货币指数，出售货币指数
     */
    public double referenceCurrencyIndex, buyCurrencyIndex, sellCurrencyIndex;

    private Objective ecoObjective;

    public DynamicEconomy() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManagers.get("config.yml").getNode("module-dynamicaleconomy", "enable").getBoolean(false)) {
            throw new ModuleExeptions.ModuleOffException();
        }
        reloadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // 反射表
        valueResourceBlockItemMap.forEach((key,value) -> valueResourceItemBlockMap.put(value, key));

        // 国库所有者是 NewNanCity
        ntOwner = ((city.newnan.newnanplus.playermanager.PlayerManager)
                plugin.getModule(city.newnan.newnanplus.playermanager.PlayerManager.class))
                .findOnePlayerByName(plugin.configManagers.get("config.yml")
                        .getNode("module-dynamicaleconomy", "owner-player").getString()).getUniqueId();

        SystemCommodity.ntOwner = plugin.getServer().getOfflinePlayer(ntOwner);
        SystemCommodity.plugin = plugin;
        SystemCommodity.dynamicEconomy = this;

        // 设置缓存文件持久化
        plugin.configManagers.setPersistent("dyneco_cache.yml");
        // 定时保存配置
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAndSave, 600L, 600L);

        // 绑定分数榜
        scoreboard = Objects.requireNonNull(plugin.getServer().getScoreboardManager()).getNewScoreboard();
        ecoObjective = scoreboard.registerNewObjective("eco", "dummy", "牛腩经济系统统计", RenderType.INTEGER);
        // 经济系统信息显示
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (displayEcoInfoPlayers.isEmpty())
                return;
            ecoObjective.unregister();
            scoreboard.clearSlot(DisplaySlot.SIDEBAR);
            ecoObjective = scoreboard.registerNewObjective("eco", "dummy", "牛腩经济系统统计", RenderType.INTEGER);
            ecoObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            // 显示信息
            ecoObjective.getScore(MessageFormat.format("系统价值总量: {0,number,#.##}", systemTotalWealth)).setScore(9);
            ecoObjective.getScore(MessageFormat.format("货币发行量: {0,number,#.##}", currencyIssuance)).setScore(8);
            ecoObjective.getScore(MessageFormat.format("国库货币储量: {0,number,#.##}", nationalTreasury)).setScore(7);
            ecoObjective.getScore(MessageFormat.format("参考货币指数: {0,number,#.##}", referenceCurrencyIndex)).setScore(6);
            ecoObjective.getScore(MessageFormat.format("收购货币指数: {0,number,#.##}", buyCurrencyIndex)).setScore(5);
            ecoObjective.getScore(MessageFormat.format("出售货币指数: {0,number,#.##}", sellCurrencyIndex)).setScore(4);
            // 放置计分板
            displayEcoInfoPlayers.forEach(player -> {
                if (player.isOnline())
                    player.setScoreboard(scoreboard);
            });
        }, 20L, 20L);

        plugin.commandManager.register("ecosidebar", this);
        plugin.commandManager.register("issue", this);
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        excludeWorld.clear();
        try {
            ConfigurationNode mainConfig = plugin.configManagers.get("config.yml").getNode("module-dynamicaleconomy");
            ConfigUtil.setListIfNull(mainConfig.getNode("exclude-world")).getList(Object::toString).
                    forEach(world -> excludeWorld.add(plugin.getServer().getWorld(world)));

            boolean ifInit = !plugin.configManagers.touch("dyneco_cache.yml");

            ConfigurationNode config = plugin.configManagers.reload("dyneco_cache.yml");
            systemTotalWealth = config.getNode("wealth", "total").getDouble();

            config.getNode("wealth", "valued-resource-count").getChildrenMap().forEach((key, value) ->
                    valueResourceCounter.put(Material.valueOf(((String) key).toUpperCase()), value.getLong()));

            if (ifInit) {
                reloadCurrencyIssuance();
            } else {
                currencyIssuance = config.getNode("currency-issuance").getDouble();
            }
            nationalTreasury = config.getNode("national-treasury").getDouble();

            systemCommodityListMap.forEach((material, systemCommodities) -> {
                systemCommodities.forEach((systemCommodity -> systemCommodity.shopList.clear()));
                systemCommodities.clear();
            });
            systemCommodityListMap.clear();

            config.getNode("commodities").getChildrenMap().forEach((key, node) -> {
                SystemCommodity commodity = new SystemCommodity(node);
                Material mKey = commodity.itemStack.getType();
                if (systemCommodityListMap.containsKey(mKey)) {
                    systemCommodityListMap.get(mKey).add(commodity);
                } else {
                    ArrayList<SystemCommodity> commoditiesList = new ArrayList<>();
                    commoditiesList.add(commodity);
                    systemCommodityListMap.put(mKey, commoditiesList);
                }
            });

            updateCurrencyIndex();
        } catch (IOException | ConfigManager.UnknownConfigFileFormatException e) {
            e.printStackTrace();
        }
    }

    public void updateAndSave() {
        try {
            updateCurrencyIndex();
            ConfigurationNode config = plugin.configManagers.get("dyneco_cache.yml");
            config.getNode("wealth", "total").setValue(systemTotalWealth);
            valueResourceCounter.forEach((material, count) ->
                    config.getNode("wealth", "valued-resource-count", material.toString()).setValue(count));
            config.getNode("currency-issuance").setValue(currencyIssuance);
            config.getNode("national-treasury").setValue(nationalTreasury);
            plugin.configManagers.save("dyneco_cache.yml");
        } catch (Exception e) {
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
        switch(token) {
            case "ecosidebar":
                toggleEconomySidebar(sender);
                break;
            case "issue":
                issueCurrency(sender, args);
        }
    }

    private void issueCurrency(CommandSender sender, String[] args) throws Exception {
        if (args.length < 1) {
            throw new CommandExceptions.BadUsageException();
        }
        if (sender instanceof ConsoleCommandSender) {
            throw new CommandExceptions.RefuseConsoleException();
        }
        issueCurrency(Double.parseDouble(args[0]));
        plugin.messageManager.printf(sender, "$module_message.dynamical_economy.issue_complete$",
                currencyIssuance, plugin.configManagers.get("config.yml").getNode("global-settings", "balance-symbol").getString());
        updateCurrencyIndex();
    }

    private void toggleEconomySidebar(CommandSender sender) {
        if (sender instanceof Player) {
            if (displayEcoInfoPlayers.contains(sender))
                displayEcoInfoPlayers.remove(sender);
            else
                displayEcoInfoPlayers.add((Player) sender);
        }
    }

    /**
     * 玩家退出时触发的方法
     * @param event 玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        displayEcoInfoPlayers.remove(event.getPlayer());
    }

    /**
     * 破坏方块掉落价值资源时，更新系统总价值量
     * @param event 破坏方块掉落物品的事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (event.isCancelled())
            return;
        if (!valueResourceBlockItemMap.containsKey(event.getBlockState().getType()))
            return;
        if (excludeWorld.contains(event.getBlock().getWorld()))
            return;
        if (!event.getPlayer().getGameMode().equals(GameMode.SURVIVAL))
            return;
        if (event.getPlayer().hasPermission("newnanplus.dynamiceconomy.statics.bypass"))
            return;
        Material sourceDropItem = event.getBlockState().getType();
        Material targetDropItem = valueResourceBlockItemMap.get(event.getBlockState().getType());
        for (Item item : event.getItems()) {
            if (item.getItemStack().getType().equals(targetDropItem) || item.getItemStack().getType().equals(sourceDropItem)) {
                systemTotalWealth += valueResourceValueMap.get(sourceDropItem);
                valueResourceCounter.put(sourceDropItem, valueResourceCounter.get(sourceDropItem) + 1);
                break;
            }
        }
    }

    /**
     * 玩家放置价值资源时，更新系统总价值量
     * @param event 玩家放置价值资源时的事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled() || !event.canBuild())
            return;
        if (!valueResourceBlockItemMap.containsKey(event.getBlockPlaced().getType()))
            return;
        if (excludeWorld.contains(event.getBlockPlaced().getWorld()))
            return;
        if (!event.getPlayer().getGameMode().equals(GameMode.SURVIVAL))
            return;
        if (event.getPlayer().hasPermission("newnanplus.dynamiceconomy.statics.bypass"))
            return;
        Material blockType = event.getBlockPlaced().getType();
        systemTotalWealth -= valueResourceValueMap.get(blockType);
        valueResourceCounter.put(blockType, valueResourceCounter.get(blockType) - 1);
    }

    /**
     * 物品被清理时，破坏方块掉落价值资源时，更新系统总价值量
     * @param event 物品被清理的事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (event.isCancelled())
            return;
        if (excludeWorld.contains(event.getLocation().getWorld()))
            return;

        ItemStack itemStack = event.getEntity().getItemStack();
        if (valueResourceBlockItemMap.containsKey(itemStack.getType())) {
            Material valueMaterial = itemStack.getType();
            int amount = itemStack.getAmount();
            systemTotalWealth -= amount * valueResourceValueMap.get(valueMaterial);
            valueResourceCounter.put(valueMaterial, valueResourceCounter.get(valueMaterial) - amount);
        }
        else if (valueResourceBlockItemMap.containsValue(itemStack.getType())) {
            Material valueMaterial = valueResourceItemBlockMap.get(itemStack.getType());
            int amount = itemStack.getAmount();
            systemTotalWealth -= amount * valueResourceValueMap.get(valueMaterial);
            valueResourceCounter.put(valueMaterial, valueResourceCounter.get(valueMaterial) - amount);
        }
    }

    // 物品以其他方式消失就不检测了，spigot这种太难做

    /**
     * 重新遍历所有的玩家、城镇、集体，统计其持有的货币总量，以更新货币发行量
     */
    public void reloadCurrencyIssuance() {
        // 统计国库
        currencyIssuance = nationalTreasury;

        // 统计玩家，去掉国库所有者
        Arrays.stream(plugin.getServer().getOfflinePlayers()).forEach(oPlayer -> {
            if (!oPlayer.getUniqueId().equals(ntOwner))
                currencyIssuance += plugin.vaultEco.getBalance(oPlayer);
        });
    }

    /**
     * Essentials的事件，玩家现金改变的时候调用，用于更新国库货币储量
     * @param event 玩家现金改变的事件
     */
    @EventHandler
    public void onUserBalanceUpdate(UserBalanceUpdateEvent event) {
        // PAY是玩家之间传递，与国库无关
        // 不过pay会调用两次(付款方、收款方)，两次之后国库是不变的，所以就算检测了也没事(
        if (event.getCause().equals(UserBalanceUpdateEvent.Cause.COMMAND_PAY))
            return;

        // 屏蔽作为国库的玩家
        if (event.getPlayer().getUniqueId().equals(ntOwner))
            return;

        // 其余的Cause都是玩家和国库的交互
        // 这里特别注意城镇。玩家->城镇这一步并不是玩家和国库的交互但是却是COMMAND_API
        // 所以后面要自己用adjustNationalTreasury做一些调整

        // 有必要用BigDecimal吗？没有
        // 国库的增量 = -玩家的增量 = 玩家的减量
        adjustNationalTreasury(event.getOldBalance().subtract(event.getNewBalance()).doubleValue());
    }

    /**
     * 调整国库货币储量数值
     * 这一步通常由上面的事件监听自动调用，但是有两个特例：
     *   - 城镇/集体成员给城镇打钱、或者从中提款，这一步没有经过国库但是却改变了国库，需要加反作用量
     *   - 城镇/集体被系统扣款，这一步没有被上面的监听自动操作，需要自己加正作用量
     * 注意：如果不是上面两个特例请不要调用这个方法，因为这个不会更新货币总发行量！
     * @param deltaValue 调整增量
     */
    public void adjustNationalTreasury(double deltaValue) {
        nationalTreasury += deltaValue;
    }

    /**
     * 在国库内发行/销毁货币，即以改变国库货币储量的方式改变货币发行量
     * 这个才是真-手动调用的那个方法
     * @param deltaValue 发行量增量，正值发行货币，负值销毁货币
     */
    public void issueCurrency(double deltaValue) {
        nationalTreasury += deltaValue;
        currencyIssuance += deltaValue;
    }

    /**
     * 手动调整系统累计价值量
     * @param deltaValue 调整增量
     */
    public void adjustSystemTotalWealth(double deltaValue) {
        systemTotalWealth += deltaValue;
    }

    /**
     * 更新货币指数
     */
    private void updateCurrencyIndex() {
        if (systemTotalWealth == 0 || currencyIssuance == 0) {
            referenceCurrencyIndex = 1.0;
        } else {
            referenceCurrencyIndex = currencyIssuance / systemTotalWealth;
        }
        buyCurrencyIndex = Math.pow(referenceCurrencyIndex, 0.691);
        sellCurrencyIndex = Math.pow(referenceCurrencyIndex, 1.309);

        systemCommodityListMap.forEach(((material, systemCommodities) ->
                systemCommodities.forEach(SystemCommodity::updateShops)));
    }

    /**
     * QuickShop 事件，玩家成功与箱子商店交易时调用，用于更新国库
     * @param event 玩家成功与箱子商店交易时的事件
     */
    @EventHandler
    public void onShopSuccessPurchase(ShopSuccessPurchaseEvent event) {
        // 检查是否为国库
        if (!event.getShop().getOwner().equals(ntOwner))
            return;

        // 钱已经在上面统计过了
        // 只需要统计国库商品库存量
        int amount = event.getAmount();
        SystemCommodity commodity = matchCommodity(event.getShop());
        assert commodity != null;
        if (event.getShop().getShopType().equals(ShopType.BUYING)) {
            // 收购商店，执行收购
            commodity.buy(amount);
        } else {
            // 售卖商店，执行售卖
            commodity.sell(amount);
        }

        // 保存商店的修改
        commodity.saveCommodityToSection();
    }

    /**
     * QuickShop 事件，商店创建时调用，用于与官方商店绑定，并加入更新队列
     * @param event 商店创建的事件
     */
    @EventHandler
    public void onShopCreate(ShopCreateEvent event) {
        // 检查是否为国库
        if (event.getShop().getOwner().equals(ntOwner))
            addToSystemShop(event.getShop());
    }

    /**
     * QuickShop 事件，商店加载时调用，用于更新系统商店的显示信息，并加入更新队列
     * 该方法和下面的方法一起，实现商店的 Lazy Bind
     * @param event 商店加载事件
     */
    @EventHandler
    public void onShopLoad(ShopLoadEvent event) {
        // 检查是否为国库
        if (event.getShop().getOwner().equals(ntOwner))
            addToSystemShop(event.getShop());
    }

    /**
     * QuickShop 事件，玩家点击箱子时调用，用于更新系统商店的显示信息，并加入更新队列
     * 和ShopLoadEvent功能重复，但是考虑到QuickShop在NewNanPlus之前加载，不排除有一些商店的ShopLoadEvent未被加载到，
     * 所以这里算是再次确认一下
     * @param event 玩家点击箱子的事件
     */
    @EventHandler
    public void onShopClick(ShopClickEvent event) {
        // 检查是否为国库
        if (event.getShop().getOwner().equals(ntOwner))
            addToSystemShop(event.getShop());
    }

    /**
     * QuickShop 事件，商店从内存中卸载时调用，从更新队列中移除
     * @param event 商店从内存中卸载的事件
     */
    @EventHandler
    public void onShopUnloadEvent(ShopUnloadEvent event) {
        // 检查是否为国库
        if (event.getShop().getOwner().equals(ntOwner))
            removeFromSystemShop(event.getShop());
    }

    /**
     * QuickShop 事件，箱子商店被删除时调用，从更新队列中移除；
     * 和Unload功能重叠但是不清楚删除箱子会不会调用ShopUnloadEvent所以这里又监听了一下
     * @param event 箱子商店被删除的事件
     */
    @EventHandler
    public void onShopDelete(ShopDeleteEvent event) {
        // 检查是否为国库
        if (event.getShop().getOwner().equals(ntOwner)) {
            Objects.requireNonNull(((ContainerShop) event.getShop()).getInventory()).clear();
            removeFromSystemShop(event.getShop());
        }
    }

    /**
     * QuickShop 事件，商店主人改变时触发
     * 如果现在的所有人是国库，那么说明之前不是，将该商店绑定国库；
     * 如果现在的所有人不是国库且存在于商店更新列表中，说明之前是，则从更新队列中移除并将商店库存清空
     * @param event 商店主人改变的事件
     */
    @EventHandler
    public void onShopModeratorChange(ShopModeratorChangedEvent event) {
        Shop shop = event.getShop();
        // 检查是否现在为国库
        if (shop.getOwner().equals(ntOwner)) {
            addToSystemShop(shop);
        } else {
            // 如果不是，则检查之前是否在列表中
            SystemCommodity commodity = matchCommodity(shop);
            if (commodity != null && commodity.shopList.contains(shop)) {
                // 如果在列表中，说明之前是国库，所以要清空库存
                commodity.shopList.remove(shop);
                Objects.requireNonNull(((ContainerShop) shop).getInventory()).clear();
            }
        }
    }

    /**
     * 添加到系统商店(对应的更新列表中)，并执行一次更新
     * @param shop 要添加的商店
     */
    public void addToSystemShop(Shop shop) {
        SystemCommodity commodity = matchCommodity(shop);
        if (commodity != null) {
            commodity.shopList.add(shop);
            commodity.updateShop(shop);
        }
    }

    /**
     * 从系统商店移除(从对应的更新列表)
     * @param shop 要移除的商店
     */
    public void removeFromSystemShop(Shop shop) {
        SystemCommodity commodity = matchCommodity(shop);
        if (commodity != null) {
            commodity.shopList.remove(shop);
        }
    }

    /**
     * 根据商店匹配对应的库存类型
     * @param shop 要匹配的商店实例
     * @return 库存实例，找不到就返回null
     */
    private SystemCommodity matchCommodity(Shop shop) {
        Material key = shop.getItem().getType();
        if (systemCommodityListMap.containsKey(key)) {
            ItemStack shopItem = shop.getItem();
            List<SystemCommodity> commoditiesList = systemCommodityListMap.get(key);
            for (SystemCommodity commodity : commoditiesList) {
                if (commodity.itemStack.isSimilar(shopItem)) {
                    return commodity;
                }
            }
        }
        return null;
    }
}

