package city.newnan.newnanplus.corn;

import city.newnan.newnanplus.NewNanPlusGlobal;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

/**
 * NewNanPlus 定时任务
 */
public class CornCommand {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal globalData;

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public CornCommand(NewNanPlusGlobal globalData) {
        this.globalData = globalData;
    }

    /**
     * 执行在服务器就绪时执行的命令，即on-server-ready
     */
    public void runOnServerReady() {
        List<String> commands = globalData.config.getStringList("module-corn.on-server-ready");
        ConsoleCommandSender sender = globalData.plugin.getServer().getConsoleSender();
        commands.forEach(command -> {
            globalData.printINFO("§a§lRun Command: §r" + command);
            globalData.plugin.getServer().dispatchCommand(sender, command);
        });
    }

    /**
     * 执行在插件禁用时执行的命令，即on-plugin-disable
     */
    public void runOnPluginDisable() {
        List<String> commands = globalData.config.getStringList("module-corn.on-plugin-disable");
        ConsoleCommandSender sender = globalData.plugin.getServer().getConsoleSender();
        commands.forEach(command -> {
            globalData.printINFO("§a§lRun Command: §r" + command);
            globalData.plugin.getServer().dispatchCommand(sender, command);
        });
    }
}
