package city.newnan.newnanplus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public interface NewNanPlusModule {
    /**
     * 重新加载模块的配置
     */
    public void reloadConfig();

    /**
     * 执行某个命令
     * @param sender  发送指令者的实例
     * @param command 被执行的指令实例
     * @param token   指令的标识字符串
     * @param args    指令的参数
     */
    public void onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String token, @NotNull String[] args) throws Exception;
}
