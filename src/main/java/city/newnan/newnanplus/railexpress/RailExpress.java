package city.newnan.newnanplus.railexpress;

import city.newnan.newnanplus.GlobalData;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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
    private final static double DEFAULT_SPEED = 0.4;
    private final GlobalData globalData;
    private final HashSet<World> excludeWorlds = new HashSet<>();
    private final HashMap<Material, Double> blockType = new HashMap<>();

    public RailExpress(GlobalData globalData) throws Exception {
        this.globalData = globalData;
        if (!globalData.configManager.get("config.yml").getBoolean("module-railexpress.enable", false)) {
            throw new ModuleOffException();
        }
        reloadConfig();
        globalData.plugin.getServer().getPluginManager().registerEvents(this, globalData.plugin);
        globalData.railExpress = this;
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        excludeWorlds.clear();
        globalData.configManager.get("config.yml").getStringList("module-railexpress.exclude-world")
                .forEach(world -> excludeWorlds.add(globalData.plugin.getServer().getWorld(world)));
        blockType.clear();
        ConfigurationSection blocks = globalData.configManager.get("config.yml")
                .getConfigurationSection("module-railexpress.block-type");
        assert blocks != null;
        blocks.getKeys(false).forEach(key -> {
            Material material = Material.getMaterial(key);
            if (material != null)
                blockType.put(material, blocks.getDouble(key));
        });
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
            // 根据下面那一块的材质确定加速比
            ((Minecart) e.getVehicle()).setMaxSpeed(DEFAULT_SPEED *
                    blockType.getOrDefault(curBlock.getRelative(BlockFace.DOWN).getType(), 1.0));
        } else {
            ((Minecart) e.getVehicle()).setMaxSpeed(DEFAULT_SPEED);
        }
    }
}
