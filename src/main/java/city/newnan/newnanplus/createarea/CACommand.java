package city.newnan.newnanplus.createarea;

import city.newnan.newnanplus.NewNanPlusGlobal;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class CACommand {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal globalData;

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public CACommand(NewNanPlusGlobal globalData) {
        this.globalData = globalData;
        this.globalData.createArea = this.globalData.plugin.loadConf("create_area.yml");
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
            globalData.sendMessage(sender, globalData.config.getString("global-data.console-selfrun-refuse"));
            return false;
        }

        Player player = (Player) sender;
        // 输入了其他玩家的名字，传送到其他玩家所在的创造区
        if (args.length > 1) {
            // 检查权限
            if (!player.hasPermission("newnanplus.ctp.other")) {
                globalData.sendPlayerMessage(player, globalData.config.getString("global-data.no-permission-msg"));
                return false;
            }
            // 查找对应的玩家
            Player _player = globalData.plugin.getServer().getPlayer(args[1]);
            // 如果找不到
            if (_player == null) {
                globalData.sendPlayerMessage(player, globalData.config.getString("global-data.player-offline-msg"));
                return false;
            }
            // 看看对应的玩家有没有创造区
            if (globalData.createArea.getConfigurationSection("areas."+_player.getUniqueId()) == null) {
                globalData.sendPlayerMessage(player, "&c玩家还有没创造区！");
                return false;
            }
            ConfigurationSection section = globalData.createArea.getConfigurationSection("areas."+_player.getUniqueId());
            int x = section.getInt("x1");
            int z = section.getInt("z1");
            World world = globalData.plugin.getServer().getWorld(globalData.createArea.getString("world"));
            Location locate = new Location(
                    world,
                    x,
                    world.getHighestBlockYAt(x, z),
                    z);
            player.teleport(locate);
            globalData.sendPlayerActionBar(player, "已到达["+_player.getName()+"]的创造区");
        } else {
            // 不带参数，传送到自己的创造区
            // 检查权限
            if (!player.hasPermission("newnanplus.ctp.self")) {
                globalData.sendPlayerMessage(player, globalData.config.getString("global-data.no-permission-msg"));
                return false;
            }
            // 看看玩家有没有创造区
            if (!globalData.createArea.isConfigurationSection("areas."+player.getUniqueId())) {
                globalData.sendPlayerMessage(player, "&c你还有没创造区！");
                return false;
            }
            ConfigurationSection section = globalData.createArea.getConfigurationSection("areas."+player.getUniqueId());
            int x = section.getInt("x1");
            int z = section.getInt("z1");
            World world = globalData.plugin.getServer().getWorld(globalData.createArea.getString("world"));
            Location locate = new Location(
                    world,
                    x,
                    world.getHighestBlockYAt(x, z),
                    z);
            player.teleport(locate);
            globalData.sendPlayerActionBar(player, "已到达你的创造区");
        }
        return true;
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
            globalData.sendMessage(sender, globalData.config.getString("global-data.no-permission-msg"));
            return false;
        }

        // 检查参数
        if (args.length < 6) {
            globalData.sendMessage(sender, globalData.config.getString("global-data.parameter-count-not-match"));
            return false;
        }

        // 查找对应的玩家
        Player _player = globalData.plugin.getServer().getPlayer(args[1]);
        // 如果找不到
        if (_player == null) {
            globalData.sendMessage(sender, globalData.config.getString("global-data.player-offline-msg"));
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

        // 获取世界名称
        String world_name = globalData.createArea.getString("world");

        // 看看玩家原来有没有创造区，有的话需要重新设置dynmap
        if (globalData.createArea.getString("areas."+player.getUniqueId()+".name") != null) {
            globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                    "dmarker deletearea id:" + player.getUniqueId());
        }

        // 地图上绘制区域
        globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x1 + " 70 " + z1 + " " + world_name);
        globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x1 + " 70 " + z2 + " " + world_name);
        globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x2 + " 70 " + z2 + " " + world_name);
        globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x2 + " 70 " + z1 + " " + world_name);
        globalData.plugin.getServer().dispatchCommand(globalData.plugin.getServer().getConsoleSender(),
                "dmarker addarea id:" + player.getUniqueId() + " " + player.getName()+"的创造区");

        // 存储玩家数据
        ConfigurationSection section = globalData.createArea.createSection("areas."+player.getUniqueId());
        section.set("name", player.getName());
        section.set("x1", x1);
        section.set("z1", z1);
        section.set("x2", x2);
        section.set("z2", z2);
        // 存储设置
        globalData.plugin.saveConf("create_area.yml", globalData.createArea);
    }
}
