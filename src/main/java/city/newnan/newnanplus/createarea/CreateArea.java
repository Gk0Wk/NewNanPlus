package city.newnan.newnanplus.createarea;

import city.newnan.newnanplus.NewNanPlusGlobal;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions.BadUsageException;
import city.newnan.newnanplus.exception.CommandExceptions.CommandExecuteException;
import city.newnan.newnanplus.exception.CommandExceptions.NoPermissionException;
import city.newnan.newnanplus.exception.CommandExceptions.PlayerOfflineException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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

        globalData.commandManager.register("ctp", this);
        globalData.commandManager.register("cnew", this);
        globalData.commandManager.register("cdel", this);
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
     * 执行某个命令
     *
     * @param sender  发送指令者的实例
     * @param command 被执行的指令实例
     * @param token   指令的标识字符串
     * @param args    指令的参数
     */
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {
        if (token.equals("ctp"))
            teleportToCreateArea(sender, args);
        if (token.equals("cnew"))
            createCreateArea(sender, args);
        if (token.equals("cdel"))
            removeCreateArea(sender, args);
    }

    /**
     * /nnp ctp指令实现，将玩家传送到自己/某人的创造区域
     * @param sender 指令发送方
     * @param args 指令参数
     */
    public void teleportToCreateArea(CommandSender sender, String args[]) throws Exception {
        FileConfiguration createArea = globalData.configManager.get("create_area.yml");

        Player player = (Player) sender;
        // 输入了其他玩家的名字，传送到其他玩家所在的创造区
        if (args.length != 0) {
            // 检查权限
            if (!player.hasPermission("newnanplus.createarea.teleport.other")) {
                throw new NoPermissionException();
            }
            // 查找对应的玩家
            Player _player = globalData.plugin.getServer().getPlayer(args[0]);
            // 如果找不到
            if (_player == null) {
                throw new PlayerOfflineException();
            }
            // 看看对应的玩家有没有创造区
            if (createArea.getConfigurationSection("areas."+_player.getUniqueId()) == null) {
                throw new CommandExecuteException("玩家没有创造区！");
            }
            _teleportTo(Objects.requireNonNull(
                    createArea.getConfigurationSection("areas." + player.getUniqueId())), player);
            globalData.sendPlayerActionBar(player, "已到达["+_player.getName()+"]的创造区");
        } else {
            // 不带参数，传送到自己的创造区
            // 检查权限
            if (!player.hasPermission("newnanplus.createarea.teleport.self")) {
                throw new NoPermissionException();
            }
            // 看看玩家有没有创造区
            if (!createArea.isConfigurationSection("areas."+player.getUniqueId())) {
                throw new CommandExecuteException("你还没有创造区！");
            }
            _teleportTo(Objects.requireNonNull(
                    createArea.getConfigurationSection("areas." + player.getUniqueId())), player);
            globalData.sendPlayerActionBar(player, "已到达你的创造区");
        }
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
     * @param args 命令参数
     */
    public void createCreateArea(CommandSender sender, String args[]) throws Exception {
        // 检查参数
        if (args.length < 5) {
            throw new BadUsageException();
        }

        // 查找对应的玩家
        Player _player;
        _player = globalData.plugin.getServer().getPlayer(args[0]);
        // 如果找不到
        if (_player == null) {
            List<Player> players = globalData.plugin.getServer().matchPlayer(args[0]);
            if (players.size() == 0) {
                throw new CommandExecuteException("找不到玩家！");
            } else if (players.size() > 1)
                throw new CommandExecuteException("找到多个玩家！");
            _player = players.get(0);
        }

        // 创建创造区域
        newCreateArea(args, _player);
    }

    /**
     * 创造/更新创造区
     * @param args 命令行参数，[PlayerName] [X1] [Z1] [X2] [Z2]
     * @param player 创造区所属玩家
     */
    public void newCreateArea(String args[], Player player) {
        // 坐标解析
        int x1 = Integer.parseInt(args[1]);
        int x2 = Integer.parseInt(args[3]);
        if (x1 > x2) {
            x1 ^= x2;
            x2 ^= x1;
            x1 ^= x2;
        }
        int z1 = Integer.parseInt(args[2]);
        int z2 = Integer.parseInt(args[4]);
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

    /**
     * /nnp cdel指令实现，删除某个玩家的创造区
     * @param sender 指令发送方
     * @param args 命令参数
     */
    public void removeCreateArea(CommandSender sender, String args[]) throws Exception {
        // 检查参数
        if (args.length < 1) {
            throw new BadUsageException();
        }

        // 查找对应的玩家
        Player player;
        player = globalData.plugin.getServer().getPlayer(args[0]);
        // 如果找不到
        if (player == null) {
            List<Player> players = globalData.plugin.getServer().matchPlayer(args[0]);
            if (players.size() == 0) {
                throw new CommandExecuteException("找不到玩家！");
            } else if (players.size() > 1)
                throw new CommandExecuteException("找到多个玩家！");
            player = players.get(0);
        }

        // 看看玩家原来有没有创造区地图标记，有的话需要先删除标记
        AreaMarker marker = createAreaMarkers.findAreaMarker(player.getUniqueId().toString());
        if (marker != null) {
            marker.deleteMarker();
        }

        // 删除配置文件对应的设置
        FileConfiguration createArea = globalData.configManager.get("create_area.yml");
        createArea.set("areas."+player.getUniqueId(), null);
        // 存储设置
        globalData.configManager.save("create_area.yml");
    }
}
