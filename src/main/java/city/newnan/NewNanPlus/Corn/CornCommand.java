package city.newnan.NewNanPlus.Corn;

import city.newnan.NewNanPlus.NewNanPlusGlobal;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

public class CornCommand {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal GlobalData;

    public CornCommand(NewNanPlusGlobal globalData) {
        GlobalData = globalData;
    }

    /**
     * 执行在插件启动时执行的命令，即on-plugin-enable
     */
    public void runOnServerReady() {
        List<String> commands = GlobalData.Config.getStringList("module-corn.on-server-ready");
        ConsoleCommandSender sender = GlobalData.Plugin.getServer().getConsoleSender();
        commands.forEach(command -> {
            GlobalData.printINFO("§a§lRun Command: §r" + command);
            GlobalData.Plugin.getServer().dispatchCommand(sender, command);
        });
    }

    /**
     * 执行在插件禁用时执行的命令，即on-plugin-disable
     */
    public void runOnPluginDisable() {
        List<String> commands = GlobalData.Config.getStringList("module-corn.on-plugin-disable");
        ConsoleCommandSender sender = GlobalData.Plugin.getServer().getConsoleSender();
        commands.forEach(command -> {
            GlobalData.printINFO("§a§lRun Command: §r" + command);
            GlobalData.Plugin.getServer().dispatchCommand(sender, command);
        });
    }
}
