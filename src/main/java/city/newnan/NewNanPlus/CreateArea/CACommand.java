package city.newnan.NewNanPlus.CreateArea;

import city.newnan.NewNanPlus.NewNanPlusGlobal;
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
    NewNanPlusGlobal GlobalData;

    public CACommand(NewNanPlusGlobal globalData) {
        GlobalData = globalData;
    }

    public boolean teleportToCreateArea(CommandSender sender, String args[]) {
        // 控制台无法执行该命令
        if (sender instanceof ConsoleCommandSender) {
            GlobalData.sendMessage(sender, GlobalData.Config.getString("global-data.console-selfrun-refuse"));
            return false;
        }

        Player player = (Player) sender;
        // 输入了其他玩家的名字，传送到其他玩家所在的创造区
        if (args.length > 1) {
            // 检查权限
            if (!player.hasPermission("newnanplus.ctp.other")) {
                GlobalData.sendPlayerMessage(player, GlobalData.Config.getString("global-data.no-permission-msg"));
                return false;
            }
            // 查找对应的玩家
            Player _player = GlobalData.Plugin.getServer().getPlayer(args[1]);
            // 如果找不到
            if (_player == null) {
                GlobalData.sendPlayerMessage(player, GlobalData.Config.getString("global-data.player-offline-msg"));
                return false;
            }
            // 看看对应的玩家有没有创造区
            if (!GlobalData.CreateArea.isConfigurationSection("areas."+_player.getUniqueId())) {
                GlobalData.sendPlayerMessage(player, "&c玩家还有没创造区！");
                return false;
            }
            ConfigurationSection section = GlobalData.CreateArea.getConfigurationSection("areas."+_player.getUniqueId());
            int x = section.getInt("x1");
            int z = section.getInt("z1");
            World world = GlobalData.Plugin.getServer().getWorld(GlobalData.CreateArea.getString("world"));
            Location locate = new Location(
                    world,
                    x,
                    world.getHighestBlockYAt(x, z),
                    z);
            player.teleport(locate);
            GlobalData.sendPlayerActionBar(player, "已到达["+_player.getName()+"]的创造区");
        } else {
            // 不带参数，传送到自己的创造区
            // 检查权限
            if (!player.hasPermission("newnanplus.ctp.self")) {
                GlobalData.sendPlayerMessage(player, GlobalData.Config.getString("global-data.no-permission-msg"));
                return false;
            }
            // 看看玩家有没有创造区
            if (!GlobalData.CreateArea.isConfigurationSection("areas."+player.getUniqueId())) {
                GlobalData.sendPlayerMessage(player, "&c你还有没创造区！");
                return false;
            }
            ConfigurationSection section = GlobalData.CreateArea.getConfigurationSection("areas."+player.getUniqueId());
            int x = section.getInt("x1");
            int z = section.getInt("z1");
            World world = GlobalData.Plugin.getServer().getWorld(GlobalData.CreateArea.getString("world"));
            Location locate = new Location(
                    world,
                    x,
                    world.getHighestBlockYAt(x, z),
                    z);
            player.teleport(locate);
            GlobalData.sendPlayerActionBar(player, "已到达你的创造区");
        }
        return true;
    }

    public boolean createCreateArea(CommandSender sender, String args[]) {
        // 检查权限
        if (!sender.hasPermission("newnanplus.cnew")) {
            GlobalData.sendMessage(sender, GlobalData.Config.getString("global-data.no-permission-msg"));
            return false;
        }

        // 检查参数
        if (args.length < 6) {
            GlobalData.sendMessage(sender, GlobalData.Config.getString("global-data.parameter-count-not-match"));
            return false;
        }

        // 查找对应的玩家
        Player _player = GlobalData.Plugin.getServer().getPlayer(args[1]);
        // 如果找不到
        if (_player == null) {
            GlobalData.sendMessage(sender, GlobalData.Config.getString("global-data.player-offline-msg"));
            return false;
        }

        // 创建创造区域
        newCreateArea(args, _player);

        return true;
    }

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
        String world_name = GlobalData.CreateArea.getString("world");

        // 看看玩家原来有没有创造区，有的话需要重新设置dynmap
        if (GlobalData.CreateArea.getString("areas."+player.getUniqueId()+".name") != null) {
            GlobalData.Plugin.getServer().dispatchCommand(GlobalData.Plugin.getServer().getConsoleSender(),
                    "dmarker deletearea id:" + player.getUniqueId());
        }

        // 地图上绘制区域
        GlobalData.Plugin.getServer().dispatchCommand(GlobalData.Plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x1 + " 70 " + z1 + " " + world_name);
        GlobalData.Plugin.getServer().dispatchCommand(GlobalData.Plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x1 + " 70 " + z2 + " " + world_name);
        GlobalData.Plugin.getServer().dispatchCommand(GlobalData.Plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x2 + " 70 " + z2 + " " + world_name);
        GlobalData.Plugin.getServer().dispatchCommand(GlobalData.Plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x2 + " 70 " + z1 + " " + world_name);
        GlobalData.Plugin.getServer().dispatchCommand(GlobalData.Plugin.getServer().getConsoleSender(),
                "dmarker addarea id:" + player.getUniqueId() + " " + player.getName()+"的创造区");

        // 存储玩家数据
        ConfigurationSection section = GlobalData.CreateArea.createSection("areas."+player.getUniqueId());
        section.set("name", player.getName());
        section.set("x1", x1);
        section.set("z1", z1);
        section.set("x2", x2);
        section.set("z2", z2);
        // 存储设置
        GlobalData.Plugin.saveConf("create_area.yml", GlobalData.CreateArea);
    }
}
