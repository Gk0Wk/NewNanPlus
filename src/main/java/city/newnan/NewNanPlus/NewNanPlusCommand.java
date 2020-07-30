package city.newnan.NewNanPlus;

import org.bukkit.Sound;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

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
                if (player.hasPermission("newnanplus.fly.other")) {
                    // 找到要操作的那个玩家
                    Player _player = Plugin.getServer().getPlayer(args[1]);
                    // 如果玩家找不到(不存在或者不在线)
                    if (_player == null) {
                        Plugin.sendPlayerMessage(player, "&c用户不在线或不存在！");
                        return true;
                    } else {
                        return makeFly(_player);
                    }
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


