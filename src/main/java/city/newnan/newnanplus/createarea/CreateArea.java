package city.newnan.newnanplus.createarea;

import city.newnan.newnanplus.NewNanPlusGlobal;
import city.newnan.newnanplus.NewNanPlusModule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

import java.util.Objects;

public class CreateArea implements NewNanPlusModule {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal globalData;

    private World createWorld;
    private MarkerSet createAreaMarkers;

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public CreateArea(NewNanPlusGlobal globalData) {
        this.globalData = globalData;

        createAreaMarkers = globalData.dynmapAPI.getMarkerAPI().getMarkerSet("NewNanPlus.CreateArea");
        if (createAreaMarkers == null) {
            createAreaMarkers = globalData.dynmapAPI.getMarkerAPI().createMarkerSet(
                    "NewNanPlus.CreateArea", "CreateArea", null, false);
        }

        reloadConfig();
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        FileConfiguration createArea = globalData.configManager.reload("create_area.yml");
        createWorld = globalData.plugin.getServer().getWorld(Objects.requireNonNull(createArea.getString("world")));

        // 检查有无没有在图中画出的创造区域
        ConfigurationSection areas = createArea.getConfigurationSection("areas");
        assert areas != null;
        for (String areaID : areas.getKeys(false)) {
            if (createAreaMarkers.findAreaMarker(areaID) == null) {
                ConfigurationSection area = areas.getConfigurationSection(areaID);
                assert area != null;
                int x1 = area.getInt("x1");
                int x2 = area.getInt("x2");
                int z1 = area.getInt("z1");
                int z2 = area.getInt("z2");
                String name = area.getString("name");
                // 地图上绘制区域
                createAreaMarkers.createAreaMarker(areaID, name+"的创造区", false,
                        createWorld.getName(), new double[]{x1, x1, x2, x2}, new double[]{z1, z2, z2, z1}, false);
            }
        }
    }

    /**
     * /nnp ct指令实现，将玩家传送到自己/某人的创造区域
     * @param sender 指令发送方
     * @param args 指令参数，包括fly
     * @return 成功执行，返回true，反之
     */
    public boolean teleportToCreateArea(CommandSender sender, String args[]) {
        // 控制台无法执行该命令
        if (sender instanceof ConsoleCommandSender) {
            globalData.sendMessage(sender, globalData.globalMessage.get("REFUSE_CONSOLE_SELFRUN"));
            return false;
        }

        FileConfiguration createArea = globalData.configManager.get("create_area.yml");

        Player player = (Player) sender;
        // 输入了其他玩家的名字，传送到其他玩家所在的创造区
        if (args.length > 1) {
            // 检查权限
            if (!player.hasPermission("newnanplus.ctp.other")) {
                globalData.sendPlayerMessage(player, globalData.globalMessage.get("NO_PERMISSION"));
                return false;
            }
            // 查找对应的玩家
            Player _player = globalData.plugin.getServer().getPlayer(args[1]);
            // 如果找不到
            if (_player == null) {
                globalData.sendPlayerMessage(player, globalData.globalMessage.get("PLAYER_OFFLINE"));
                return false;
            }
            // 看看对应的玩家有没有创造区
            if (createArea.getConfigurationSection("areas."+_player.getUniqueId()) == null) {
                globalData.sendPlayerMessage(player, "&c玩家还有没创造区！");
                return false;
            }
            _teleportTo(Objects.requireNonNull(
                    createArea.getConfigurationSection("areas." + player.getUniqueId())), player);
            globalData.sendPlayerActionBar(player, "已到达["+_player.getName()+"]的创造区");
        } else {
            // 不带参数，传送到自己的创造区
            // 检查权限
            if (!player.hasPermission("newnanplus.ctp.self")) {
                globalData.sendPlayerMessage(player, globalData.globalMessage.get("NO_PERMISSION"));
                return false;
            }
            // 看看玩家有没有创造区
            if (!createArea.isConfigurationSection("areas."+player.getUniqueId())) {
                globalData.sendPlayerMessage(player, "&c你还有没创造区！");
                return false;
            }
            _teleportTo(Objects.requireNonNull(
                    createArea.getConfigurationSection("areas." + player.getUniqueId())), player);
            globalData.sendPlayerActionBar(player, "已到达你的创造区");
        }
        return true;
    }

    private void _teleportTo(ConfigurationSection areaSection, Player player) {
        int x =  areaSection.getInt("x1");
        int z =  areaSection.getInt("z1");
        Location locate = new Location(
                createWorld,
                x,
                createWorld.getHighestBlockYAt(x, z),
                z);
        player.teleport(locate);
    }

    /**
     * /nnp cnew指令实现，创建某个玩家的创造区
     * @param sender 指令发送方
     * @param args 命令参数，包括cnew
     * @return 成功执行，返回true，反之
     */
    public boolean createCreateArea(CommandSender sender, String args[]) {
        // 检查权限
        if (!sender.hasPermission("newnanplus.cnew")) {
            globalData.sendMessage(sender, globalData.globalMessage.get("NO_PERMISSION"));
            return false;
        }

        // 检查参数
        if (args.length < 6) {
            globalData.sendMessage(sender, globalData.globalMessage.get("PARAMETER_NUMBER_NOT_MATCH"));
            return false;
        }

        // 查找对应的玩家
        Player _player = globalData.plugin.getServer().getPlayer(args[1]);
        // 如果找不到
        if (_player == null) {
            globalData.sendMessage(sender, globalData.globalMessage.get("PLAYER_OFFLINE"));
            return false;
        }

        // 创建创造区域
        newCreateArea(args, _player);

        return true;
    }

    /**
     * 创造/更新创造区
     * @param args 命令行参数，cnew [PlayerName] [X1] [Z1] [X2] [Z2]
     * @param player 创造区所属玩家
     */
    public void newCreateArea(String args[], Player player) {
        // 坐标解析
        int x1 = Integer.parseInt(args[2]);
        int x2 = Integer.parseInt(args[4]);
        if (x1 > x2) {
            x1 ^= x2;
            x2 ^= x1;
            x1 ^= x2;
        }
        int z1 = Integer.parseInt(args[3]);
        int z2 = Integer.parseInt(args[5]);
        if (z1 > z2) {
            z1 ^= z2;
            z2 ^= z1;
            z1 ^= z2;
        }

        FileConfiguration createArea = globalData.configManager.get("create_area.yml");

        // 看看玩家原来有没有创造区地图标记，有的话需要先删除标记
        AreaMarker marker = createAreaMarkers.findAreaMarker(player.getUniqueId().toString());
        if (marker != null) {
            marker.deleteMarker();
        }

        // 地图上绘制区域
        createAreaMarkers.createAreaMarker(player.getUniqueId().toString(), player.getName()+"的创造区", false,
                createWorld.getName(), new double[]{x1, x1, x2, x2}, new double[]{z1, z2, z2, z1}, false);

        // 存储玩家数据
        ConfigurationSection section = createArea.createSection("areas."+player.getUniqueId());
        section.set("name", player.getName());
        section.set("x1", x1);
        section.set("z1", z1);
        section.set("x2", x2);
        section.set("z2", z2);
        // 存储设置
        globalData.configManager.save("create_area.yml");
    }
}
