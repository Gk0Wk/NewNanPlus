package city.newnan.newnanplus.teleport;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Teleport implements NewNanPlusModule {
    private final NewNanPlus plugin;
    /**
     * 会话缓存实例
     */
    SessionCache sessionCache;

    public Teleport() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManager.get("config.yml").getBoolean("module-teleport.enable.enable", false))
            throw new ModuleExeptions.ModuleOffException();
        sessionCache = new SessionCache();
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
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
    public void executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {

    }
}
