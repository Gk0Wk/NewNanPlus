package city.newnan.NewNanPlus;

import org.bukkit.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NewNanPlusCommand implements CommandExecutor {
    /**
     * 插件对象，用于持久化存储和访问全局数据
     */
    private NewNanPlusPlugin Plugin = null;

    public NewNanPlusCommand(NewNanPlusPlugin plugin) {
        Plugin = plugin;
    }

    /**
     * 接收到命令就会调用这个，为了简单，把所有命令都注册到这里，再用label分流到各自的执行函数
     * @param sender 发送者，控制台、命令方块、玩家或者其他实体
     * @param cmd 命令本身的属性
     * @param label 命令名
     * @param args 参数
     * @return 玩家正确使用了命令，返回true，反之
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // commandSender - 命令的发送者，可以是玩家、控制台或命令方块
        // command - 发送命令的一些信息，其实就是plugin中配置的一些信息，感觉这个参数没啥作用呀
        // label - 玩家敲的命令，会自动去掉'/'，这样可以把多个命令绑在一个函数上
        // args - 玩家敲的命令后面跟的参数

        // 不允许其他的对象来请求执行这个命令
        if (!(sender instanceof ConsoleCommandSender || sender instanceof Player)) {
            return false;
        }

        if (args.length == 0) {
            // /nnp 指令
        } else {
            switch(args[0]) {
                case "fly": return applyFly(sender, args);
                case "reload":  return reloadConfig(sender);
                case "save": return saveConfig(sender);
                case "allow": return allowNewbieToPlayer(sender, args);
                case "ctp": return teleportToCreateArea(sender, args);
                case "cnew": return createCreateArea(sender, args);
            }
        }
        return false;
    }

    /**
     * /nnp fly 启动付费飞行模式
     * @param sender 发送者实例
     * @param args 参数
     * @return 正确使用了命令，返回true，反之
     */
    private boolean applyFly(CommandSender sender, String[] args) {
        // 给别人开启
        if (args.length > 1) {
            // 如果输入命令的是玩家
            if (sender instanceof Player) {
                Player player = (Player) sender;
                // 检查他的权限
                if (!player.hasPermission("newnanplus.fly.other")) {
                    Plugin.sendPlayerMessage(player, "&c你没有这样做的权限！");
                    return false;
                }
                // 找到要操作的那个玩家
                Player _player = Plugin.getServer().getPlayer(args[1]);
                // 如果玩家找不到(不存在或者不在线)
                if (_player == null) {
                    Plugin.sendPlayerMessage(player, "&c用户不在线或不存在！");
                    return true;
                } else {
                    return makeFly(_player);
                }
            } else {
                // 找到要操作的那个玩家
                Player _player = Plugin.getServer().getPlayer(args[1]);
                // 如果玩家找不到(不存在或者不在线
                if (_player == null) {
                    Plugin.printINFO("&c用户不在线或不存在！");
                    return true;
                } else {
                    return makeFly(_player);
                }
            }
        } else {
            // 没有参数，就是给自己
            // 先要判断是不是玩家，不是不行
            if (sender instanceof Player) {
                Player player = (Player) sender;
                // 如果他有自己的飞行权限
                if (player.hasPermission("newnanplus.fly.self")) {
                    return makeFly(player);
                } else {
                    Plugin.sendPlayerMessage(player, "&c你没有这样做的权限！");
                }
            }
        }
        return false;
    }

    /**
     * 让一个玩家进入付费飞行模式
     * @param player 玩家实例
     * @return 成功开启/关闭返回true，反之
     */
    private boolean makeFly(Player player) {
        // 检查这个玩家是否在飞行
        if (Plugin.FlyingPlayers.contains(player)) {
            // 在飞行，就取消飞行
            Plugin.cancelFly(player, true);
            Plugin.sendPlayerMessage(player, Plugin.getConf().getString("module-flyfee.msg-finish"));
            Plugin.sendPlayerActionBar(player, Plugin.getConf().getString("module-flyfee.msg-finish"));
        } else {
            // 不在飞行，就开启飞行
            // 现金大于零才能飞
            if (Plugin.getVaultEco().getBalance(player) > 0.0) {
                player.setFlySpeed((float)Plugin.getConf().getDouble("module-flyfee.fly-speed"));
                player.setAllowFlight(true);
                Plugin.FlyingPlayers.add(player);
                Plugin.sendPlayerMessage(player, Plugin.getConf().getString("module-flyfee.msg-begin"));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.0f);
            } else {
                // 不大于零就提示不能飞
                Plugin.sendPlayerMessage(player, Plugin.getConf().getString("module-flyfee.msg-nofee"));
            }
        }
        return true;
    }

    private boolean allowNewbieToPlayer(CommandSender sender, String args[]) {
        // 检查权限
        if (!sender.hasPermission("nnp.newbies.allow")) {
            Plugin.sendMessage(sender, "&c你没有这样做的权限！");
        }

        // 寻找目标玩家
        Player player = Plugin.getServer().getPlayer(args[1]);
        // 是否需要刷新新人名单
        boolean need_refresh = false;

        // 如果玩家不在线或不存在，就存到配置里
        if (player == null) {
            // 获取未通过的新人组的List
            List<String> list_not = (List<String>) Plugin.NewbiesList.getList("not-passed-newbies");
            // 如果未通过里有这个玩家，那就去掉
            if (list_not.contains(args[1])) {
                list_not.remove(args[1]);
                Plugin.NewbiesList.set("not-passed-newbies", list_not);
                need_refresh = true;
            }
            // 获取已通过的新人组的List
            List<String> list_yet = (List<String>) Plugin.NewbiesList.getList("yet-passed-newbies");
            // 如果通过里没有这个玩家，就加入
            if (!list_yet.contains(args[1])) {
                list_yet.add(args[1]);
                Plugin.NewbiesList.set("yet-passed-newbies", list_yet);
                need_refresh = true;
            }
            Plugin.sendMessage(sender, "操作成功，玩家未上线，稍后玩家登录后将自动获得权限。");
        } else {
            // 如果玩家在线，就直接赋予权限
            // 但是要检查一下原来所在的组
            if (Plugin.getVaultPerm().getPrimaryGroup(player).toLowerCase()
                    .equals(Plugin.getConf().getString("module-allownewbies.newbies-group"))) {
                Plugin.getServer().dispatchCommand(Plugin.getServer().getConsoleSender(),
                        "manuadd " + player.getName()
                                + " " + Plugin.getConf().getString("module-allownewbies.player-group")
                                + " " + Plugin.getConf().getString("module-allownewbies.world-group"));
                // 获取未通过的新人组的List
                List<String> list_not = (List<String>) Plugin.NewbiesList.getList("not-passed-newbies");
                // 如果未通过里有这个玩家，那就去掉
                if (list_not.contains(player.getName())) {
                    list_not.remove(player.getName());
                    Plugin.NewbiesList.set("not-passed-newbies", list_not);
                    need_refresh = true;
                }
                Plugin.sendMessage(sender, "操作成功，赋予玩家权限。");
            } else {
                Plugin.sendMessage(sender, "该玩家已不是新人。");
            }
        }
        if (need_refresh) {
            Plugin.saveConf("newbies_list.yml", Plugin.NewbiesList);
        }
        return true;
    }

    /**
     * 保存配置
     * @param sender 命令发送者实例
     * @return 正确使用了命令，返回true，反之
     */
    private boolean saveConfig(CommandSender sender) {
        if (sender.hasPermission("newnanplus.save")) {
            Plugin.saveConfig();
            return true;
        }
        return false;
    }

    /**
     * 重载配置
     * @param sender 命令发送者实例
     * @return 正确使用了命令，返回true，反之
     */
    private boolean reloadConfig(CommandSender sender) {
        if (sender.hasPermission("newnanplus.reload")) {
            Plugin.reloadConfig();
            return true;
        }
        return false;
    }

    private boolean teleportToCreateArea(CommandSender sender, String args[]) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // 输入了其他玩家的名字，传送到其他玩家所在的创造区
            if (args.length > 1) {
                // 检查权限
                if (!player.hasPermission("newnanplus.ctp.other")) {
                    Plugin.sendPlayerMessage(player, "&c你没有这样做的权限！");
                    return false;
                }
                // 查找对应的玩家
                Player _player = Plugin.getServer().getPlayer(args[1]);
                // 如果找不到
                if (_player == null) {
                    Plugin.sendPlayerMessage(player, "&c玩家未在线或不存在！");
                    return false;
                }
                // 看看对应的玩家有没有创造区
                if (!Plugin.CreateArea.isConfigurationSection("areas."+_player.getUniqueId())) {
                    Plugin.sendPlayerMessage(player, "&c玩家还有没创造区！");
                    return false;
                }
                ConfigurationSection section = Plugin.CreateArea.getConfigurationSection("areas."+_player.getUniqueId());
                int x = section.getInt("x1");
                int z = section.getInt("z1");
                World world = Plugin.getServer().getWorld(Plugin.CreateArea.getString("world"));
                Location locate = new Location(
                        world,
                        x,
                        world.getHighestBlockYAt(x, z),
                        z);
                player.teleport(locate);
                NewNanPlusPlugin.sendPlayerActionBar(player, "已到达["+_player.getName()+"]的创造区");
            } else {
                // 不带参数，传送到自己的创造区
                // 检查权限
                if (!player.hasPermission("newnanplus.ctp.self")) {
                    Plugin.sendPlayerMessage(player, "&c你没有这样做的权限！");
                    return false;
                }
                // 看看玩家有没有创造区
                if (!Plugin.CreateArea.isConfigurationSection("areas."+player.getUniqueId())) {
                    Plugin.sendPlayerMessage(player, "&c你还有没创造区！");
                    return false;
                }
                ConfigurationSection section = Plugin.CreateArea.getConfigurationSection("areas."+player.getUniqueId());
                int x = section.getInt("x1");
                int z = section.getInt("z1");
                World world = Plugin.getServer().getWorld(Plugin.CreateArea.getString("world"));
                Location locate = new Location(
                        world,
                        x,
                        world.getHighestBlockYAt(x, z),
                        z);
                player.teleport(locate);
                NewNanPlusPlugin.sendPlayerActionBar(player, "已到达你的创造区");
            }
        } else {
            Plugin.printINFO("&c该指令只能由玩家执行(控制台用这个想去哪)！");
            return false;
        }
        return true;
    }

    private boolean createCreateArea(CommandSender sender, String args[]) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("newnanplus.cnew")) {
                Plugin.sendPlayerMessage(player, "&c你没有这样做的权限！");
                return false;
            }
            if (args.length < 6) {
                Plugin.sendPlayerMessage(player, "&c参数数量不匹配！");
                return false;
            }
            // 查找对应的玩家
            Player _player = Plugin.getServer().getPlayer(args[1]);
            // 如果找不到
            if (_player == null) {
                Plugin.sendPlayerMessage(player, "&c玩家未在线或不存在！");
                return false;
            }
            newCreateArea(args, _player);
        } else {
            if (args.length < 6) {
                Plugin.printINFO("&c参数数量不匹配！");
                return false;
            }
            // 查找对应的玩家
            Player player = Plugin.getServer().getPlayer(args[1]);
            // 如果找不到
            if (player == null) {
                Plugin.printINFO("&c玩家未在线或不存在！");
                return false;
            }
            newCreateArea(args, player);
        }
        return true;
    }

    private void newCreateArea(String args[], Player player) {
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
        // 看看玩家原来有没有创造区，有的话需要重新设置dynmap
        if (Plugin.CreateArea.getString("areas."+player.getUniqueId()+".name") != null) {
            Plugin.getServer().dispatchCommand(Plugin.getServer().getConsoleSender(),
                    "dmarker deletearea id:" + player.getUniqueId());
        }
        // 存储玩家数据
        ConfigurationSection section = Plugin.CreateArea.createSection("areas."+player.getUniqueId());
        section.set("name", player.getName());
        section.set("x1", x1);
        section.set("z1", z1);
        section.set("x2", x2);
        section.set("z2", z2);
        // 地图上绘制区域
        Plugin.printINFO("dmarker addcorner " + x1 + " 70 " + z1 + " " + Plugin.CreateArea.getString("world"));
        Plugin.getServer().dispatchCommand(Plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x1 + " 70 " + z1 + " " + Plugin.CreateArea.getString("world"));
        Plugin.getServer().dispatchCommand(Plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x1 + " 70 " + z2 + " " + Plugin.CreateArea.getString("world"));
        Plugin.getServer().dispatchCommand(Plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x2 + " 70 " + z2 + " " + Plugin.CreateArea.getString("world"));
        Plugin.getServer().dispatchCommand(Plugin.getServer().getConsoleSender(),
                "dmarker addcorner " + x2 + " 70 " + z1 + " " + Plugin.CreateArea.getString("world"));
        Plugin.getServer().dispatchCommand(Plugin.getServer().getConsoleSender(),
                "dmarker addarea id:" + player.getUniqueId() + " " + player.getName()+"的创造区");
        // 存储设置
        Plugin.saveConf("create_area.yml", Plugin.CreateArea);
    }

    private boolean testFunction(CommandSender sender, String[] args) {
        // 如果是玩家
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Create a new ItemStack (type: diamond)
            ItemStack diamond = new ItemStack(Material.DIAMOND);

            // Create a new ItemStack (type: brick)
            ItemStack bricks = new ItemStack(Material.BRICK);

            // Set the amount of the ItemStack
            bricks.setAmount(20);

            // Give the player our items (comma-seperated list of all ItemStack)
            player.getInventory().addItem(bricks, diamond);
        }
        return true;
    }
}


