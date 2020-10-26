package city.newnan.newnanplus.powertools;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions;
import me.wolfyscript.utilities.api.WolfyUtilities;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PowerTools implements NewNanPlusModule {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    public PowerTools() {
        plugin = NewNanPlus.getPlugin();
        plugin.commandManager.register("titlemsg", this);
        plugin.commandManager.register("titlebroadcast", this);
        plugin.commandManager.register("whois", this);
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
        if (token.equals("titlemsg"))
            sendTitleMessage(args);
        else if (token.equals("titlebroadcast"))
            sendTitleBroadcast(args);
        else if (token.equals("whois"))
            lookupPlayer(sender, args);
    }

    private void sendTitleMessage(String[] args) throws Exception {
        // 检查参数
        if (args.length < 2 || args.length > 4) {
            throw new CommandExceptions.BadUsageException();
        }
        // 找玩家
        Player player = NewNanPlus.getPlugin().getServer().getPlayer(args[0]);
        if (player == null) {
            throw new CommandExceptions.PlayerNotFountException();
        }

        // 寻找标题
        String title = null;
        String subTitle = null;
        Sound sound = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].matches("^title:.*")) {
                title = WolfyUtilities.translateColorCodes(args[i].replaceFirst("^title:", ""));
            }
            else if (args[i].matches("^subtitle:.*")) {
                subTitle = WolfyUtilities.translateColorCodes(args[i].replaceFirst("^subtitle:", ""));
            }
            else if (args[i].matches("^sound:.*")) {
                sound = Sound.valueOf(args[i].replaceFirst("^sound:", ""));
            }
        }

        if (title != null || subTitle != null)
            player.sendTitle(title, subTitle, 3, 37, 2);
        if (sound != null)
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    private void sendTitleBroadcast(String[] args) throws Exception {
        // 检查参数
        if (args.length < 1 || args.length > 3) {
            throw new CommandExceptions.BadUsageException();
        }

        // 寻找标题
        String title = null;
        String subTitle = null;
        Sound sound = null;
        for (String arg : args) {
            if (arg.matches("^title:.*")) {
                title = WolfyUtilities.translateColorCodes(arg.replaceFirst("^title:", ""));
            }
            else if (arg.matches("^subtitle:.*")) {
                subTitle = WolfyUtilities.translateColorCodes(arg.replaceFirst("^subtitle:", ""));
            }
            else if (arg.matches("^sound:.*")) {
                sound = Sound.valueOf(arg.replaceFirst("^sound:", ""));
            }
        }

        String finalTitle = title;
        String finalSubTitle = subTitle;
        Sound finalSound = sound;
        NewNanPlus.getPlugin().getServer().getOnlinePlayers().forEach(player -> {
            if (!player.hasPermission("newnanplus.powertools.titlebroadcast.bypass")) {
                if (finalTitle != null || finalSubTitle != null)
                    player.sendTitle(finalTitle, finalSubTitle, 3, 37, 2);
                if (finalSound != null)
                    player.playSound(player.getLocation(), finalSound, 1.0f, 1.0f);
            }
        });
    }

    private void lookupPlayer(CommandSender sender, String[] args) throws Exception {
        if (args.length != 1) {
            throw new CommandExceptions.BadUsageException();
        }
        List<OfflinePlayer> players = null;
        if (args[0].matches("^name:.*")) {
            String name = args[0].replaceFirst("^name:", "");
            players = Arrays.stream(plugin.getServer().getOfflinePlayers()).
                    filter(oPlayer -> Objects.equals(oPlayer.getName(), name)).collect(Collectors.toList());
        }
        else if (args[0].matches("^uuid:.*")){
            String uuid = args[0].replaceFirst("^uuid:", "");
            for (OfflinePlayer oPlayer : plugin.getServer().getOfflinePlayers()) {
                if (oPlayer.getUniqueId().toString().equals(uuid)) {
                    players = new ArrayList<>();
                    players.add(oPlayer);
                    break;
                }
            }
        } else {
            throw new CommandExceptions.BadUsageException();
        }

        if (players == null || players.size() == 0) {
            throw new CommandExceptions.PlayerNotFountException();
        }

        for (OfflinePlayer player : players) {
            plugin.messageManager.sendMessage(sender, MessageFormat.format(
                    plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.power_tools.user_display_format$"),
                    player.getName(), player.getUniqueId().toString()));
        }
    }
}
