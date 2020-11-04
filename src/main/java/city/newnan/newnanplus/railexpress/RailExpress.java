package city.newnan.newnanplus.railexpress;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;

/**
 * 矿车加速模块，根据激活铁轨下不同的材质进行加速
 */
public class RailExpress implements NewNanPlusModule, Listener {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    /**
     * 游戏默认矿车极速为0.4，最高为1.5
     */
    private final static double DEFAULT_SPEED = 0.4;
    private final HashSet<World> excludeWorlds = new HashSet<>();
    private final HashMap<Material, Double> blockSpeedMap = new HashMap<>();

    public RailExpress() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManager.get("config.yml").getBoolean("module-railexpress.enable", false)) {
            throw new ModuleOffException();
        }
        reloadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        FileConfiguration config = plugin.configManager.get("config.yml");
        excludeWorlds.clear();
        config.getStringList("module-railexpress.exclude-world")
                .forEach(world -> excludeWorlds.add(plugin.getServer().getWorld(world)));

        blockSpeedMap.clear();
        ConfigurationSection blockSpeedSection = config.getConfigurationSection("module-railexpress.block-type");
        assert blockSpeedSection != null;
        blockSpeedSection.getKeys(false).forEach(block -> blockSpeedMap.
                put(Material.valueOf(block.toUpperCase()), blockSpeedSection.getDouble(block, DEFAULT_SPEED)));
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
     * 实体离开交通工具时触发，复原矿车速度，以免追不上车
     * @param e 实体离开交通工具的事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onVehicleExit(VehicleExitEvent e) {
        // 是否是矿车
        if (!(e.getVehicle() instanceof Minecart))
            return;
        // 检查世界
        if (excludeWorlds.contains(e.getVehicle().getWorld()))
            return;
        // 看矿车只能坐一个实体，所以退出的就是刚才的唯一的实体
        // if (e.getVehicle().isEmpty())
        ((Minecart) e.getVehicle()).setMaxSpeed(DEFAULT_SPEED);
    }

    /**
     * 交通工具移动时触发，检测矿车、所在世界，如果跑在激活铁轨上，就根据铁轨下面的方块确定加速比
     * @param e 交通工具移动事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    void onVehicleMove(VehicleMoveEvent e) {
        // 是否是矿车
        if (!(e.getVehicle() instanceof Minecart))
            return;
        // 检查世界
        if (excludeWorlds.contains(e.getVehicle().getWorld()))
            return;
        // 空车不加速
        if (e.getVehicle().isEmpty())
            return;

        // 需要是激活铁轨才加速，否则重置成原速
        Block curBlock = e.getVehicle().getLocation().getBlock();
        if (curBlock.getType().equals(Material.POWERED_RAIL)) {
            // 看看铁轨下面的方块是什么，赋予相应的速度
            Block belowBlock = curBlock.getRelative(BlockFace.DOWN);
            ((Minecart) e.getVehicle()).setMaxSpeed(blockSpeedMap.getOrDefault(belowBlock.getType(), DEFAULT_SPEED));
        }
        // 否则设置为默认速度
        else ((Minecart) e.getVehicle()).setMaxSpeed(DEFAULT_SPEED);
    }
}