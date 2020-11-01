package city.newnan.newnanplus.dynamiceconomy;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class DynamicEconomy implements NewNanPlusModule, Listener {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    private final HashSet<World> excludeWorld = new HashSet<>();

    /**
     * 价值资源矿与价值资源的对应
     */
    private static final HashMap<Material, Material> valueResourceBlockItemMap = new HashMap<>() {{
        put(Material.COAL_ORE, Material.COAL);             // 煤矿 -> 煤炭
        put(Material.IRON_ORE, Material.IRON_INGOT);       // 铁矿 -> 铁锭
        put(Material.GOLD_ORE, Material.GOLD_INGOT);       // 金矿 -> 金锭
        put(Material.REDSTONE_ORE, Material.REDSTONE);     // 红石矿 -> 红石
        put(Material.LAPIS_ORE, Material.LAPIS_LAZULI);    // 青金石矿 -> 青金石
        put(Material.DIAMOND_ORE, Material.DIAMOND);       // 钻石矿 -> 钻石
        put(Material.EMERALD_ORE, Material.EMERALD);       // 绿宝石矿 -> 绿宝石
        put(Material.NETHER_QUARTZ_ORE, Material.QUARTZ);  // 下界石英矿 -> 下界石英
    }};

    /**
     * 价值资源的价值量
     */
    private static final HashMap<Material, Double> valueResourceValueMap = new HashMap<>() {{
        put(Material.COAL, 1.7);             // 煤炭
        put(Material.IRON_INGOT, 3.2);       // 铁锭
        put(Material.GOLD_INGOT, 12.5);       // 金锭
        put(Material.REDSTONE, 35.0);         // 红石
        put(Material.LAPIS_LAZULI, 90.0);     // 青金石
        put(Material.DIAMOND, 100.0);          // 钻石
        put(Material.EMERALD, 620.0);          // 绿宝石
        put(Material.QUARTZ, 1.0);           // 下界石英
    }};

    /**
     * 价值资源的采集总量
     */
    private final HashMap<Material, Integer> valueResourceCounter = new HashMap<>() {{
        put(Material.COAL, 0);             // 煤炭
        put(Material.IRON_INGOT, 0);       // 铁锭
        put(Material.GOLD_INGOT, 0);       // 金锭
        put(Material.REDSTONE, 0);         // 红石
        put(Material.LAPIS_LAZULI, 0);     // 青金石
        put(Material.DIAMOND, 0);          // 钻石
        put(Material.EMERALD, 0);          // 绿宝石
        put(Material.QUARTZ, 0);           // 下界石英
    }};

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
    private double referenceCurrencyIndex, buyCurrencyIndex, sellCurrencyIndex;

    public DynamicEconomy() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManager.get("config.yml").getBoolean("module-dynamicaleconomy.enable", false)) {
            throw new ModuleExeptions.ModuleOffException();
        }
        reloadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
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
        if (event.getPlayer().hasPermission("newnanplus.dynamiceconomy.statics.bypass"))
            return;
        Material targetDropItem = valueResourceBlockItemMap.get(event.getBlockState().getType());
        for (Item item : event.getItems()) {
            if (item.getItemStack().getType().equals(targetDropItem)) {
                systemTotalWealth += item.getItemStack().getAmount() * valueResourceValueMap.get(targetDropItem);
                valueResourceCounter.put(targetDropItem, valueResourceCounter.get(targetDropItem) + item.getItemStack().getAmount());
                break;
            }
        }
    }

    /**
     * 物品被清理时，破坏方块掉落价值资源时，更新系统总价值量
     * @param event 物品被清理的事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (event.isCancelled())
            return;
        if (!valueResourceBlockItemMap.containsKey(event.getEntity().getItemStack().getType()))
            return;
        if (excludeWorld.contains(event.getLocation().getWorld()))
            return;

        ItemStack itemStack = event.getEntity().getItemStack();
        systemTotalWealth -= itemStack.getAmount() * valueResourceValueMap.get(itemStack.getType());
        valueResourceCounter.put(itemStack.getType(), valueResourceCounter.get(itemStack.getType()) - itemStack.getAmount());
    }

    // 物品以其他方式消失就不检测了，spigot这种太难做

    /**
     * 重新遍历所有的玩家、城镇、集体，统计其持有的货币总量，以更新货币发行量
     */
    public void reloadCurrencyIssuance() {
        // 统计国库
        currencyIssuance = nationalTreasury;

        // 统计玩家
        Arrays.stream(plugin.getServer().getOfflinePlayers()).forEach(
                oPlayer -> currencyIssuance += plugin.vaultEco.getBalance(oPlayer));

        // 统计城镇
        ((city.newnan.newnanplus.town.TownManager) plugin.getModule(city.newnan.newnanplus.town.TownManager.class))
                .getTowns().forEach(town -> currencyIssuance += town.balance);

        // 统计集体(?不搞了不搞了，太肝了)
    }

    /**
     * 玩家现金改变的时候调用，用于更新国库货币储量
     * @param event 玩家现金改变的事件，是Essentials的事件
     */
    @EventHandler
    public void onUserBalanceUpdate(UserBalanceUpdateEvent event) {
        // PAY是玩家之间传递，与国库无关
        // 不过pay会调用两次(付款方、收款方)，两次之后国库是不变的，所以就算检测了也没事(
        if (event.getCause().equals(UserBalanceUpdateEvent.Cause.COMMAND_PAY))
            return;

        // 其余的Cause都是玩家和国库的交互
        // 这里特别注意城镇。玩家->城镇这一步并不是玩家和国库的交互但是却是COMMAND_API
        // 所以后面要自己用adjustNationalTreasury做一些调整

        // 有必要用BigDecimal吗？暂时不用
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
        referenceCurrencyIndex = systemTotalWealth / currencyIssuance;
        buyCurrencyIndex = Math.pow(referenceCurrencyIndex, 0.691);
        sellCurrencyIndex = Math.pow(referenceCurrencyIndex, 1.309);
    }
}

class SystemCommodity {
    ItemStack itemStack;
}