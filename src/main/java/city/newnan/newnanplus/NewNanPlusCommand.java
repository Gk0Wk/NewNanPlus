package city.newnan.newnanplus;

import me.wolfyscript.utilities.api.inventory.GuiHandler;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 全局的命令监听和执行类，解析命令并调用各模块的方法，
 * 和全局事件监听类一样不适合可拆卸模块设计，未来可能会被替换掉。
 */
public class NewNanPlusCommand implements CommandExecutor {
    /**
     * 用于持久化存储和访问全局数据
     */
    private final NewNanPlusGlobal globalData;

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public NewNanPlusCommand(NewNanPlusGlobal globalData) {
        this.globalData = globalData;
        this.globalData.plugin.getCommand("nnp").setExecutor(this);
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
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
            return nnpMainCommand(sender, args);
        } else {
            switch(args[0].toLowerCase()) {
                case "fly":
                    return globalData.feeFly.applyFly(sender, args);
                case "reload":
                    return reloadConfig(sender);
                case "save":
                    return saveConfig(sender);
                case "allow":
                    return globalData.playerManager.allowNewbieToPlayer(sender, args);
                case "ctp":
                    return globalData.createArea.teleportToCreateArea(sender, args);
                case "cnew":
                    return globalData.createArea.createCreateArea(sender, args);
                case "genhopper":
                    return globalData.lagAnalyzer.genHopperReport();
                case "list":
                    return listSomething(sender, args);
                default:
                    return nnpMainCommand(sender, args);
            }
        }
    }

    private boolean nnpMainCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {

            GuiHandler h = globalData.wolfyInventoryAPI.getGuiHandler((Player) sender);
            h.setButton(globalData.wolfyInventoryAPI.getGuiWindow("none", "main_menu"), 1, "settings");
            globalData.wolfyInventoryAPI.openGui((Player)sender, "none", "main_menu");
        }
        return true;
    }

    /**
     * 保存配置
     * @param sender 命令发送者实例
     * @return 正确使用了命令，返回true，反之
     */
    private boolean saveConfig(CommandSender sender) {
        if (!sender.hasPermission("newnanplus.save")) {
            globalData.sendMessage(sender, globalData.globalMessage.get("NO_PERMISSION"));
            return false;
        }
        globalData.plugin.saveConfig();
        return true;
    }

    /**
     * 重载配置
     * @param sender 命令发送者实例
     * @return 正确使用了命令，返回true，反之
     */
    private boolean reloadConfig(CommandSender sender) {
        if (!sender.hasPermission("newnanplus.reload")) {
            globalData.sendMessage(sender, globalData.globalMessage.get("NO_PERMISSION"));
            return false;
        }
        globalData.plugin.reloadConfig();
        return true;
    }

    /**
     * 输出一些插件信息
     * @param sender 命令发送者实例
     * @param args   命令参数数组
     * @return 正确使用了命令，返回true，反之
     */
    private boolean listSomething(CommandSender sender, String[] args) {
        if (args.length < 2) {
            globalData.sendMessage(sender, "&c参数不匹配！");
            return false;
        }
        if (args[1].equalsIgnoreCase("fly")) {
            return globalData.feeFly.listFlyingPlayers(sender);
        }
        return true;
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


