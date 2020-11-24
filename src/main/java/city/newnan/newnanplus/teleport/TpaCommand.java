package city.newnan.newnanplus.teleport;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.exception.CommandExceptions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpaCommand {
    /**
     * Tpa实例
     */
    private static Tpa tpaInstance;

    /**
     * 初始化
     * @param instance Tpa实例
     */
    public static void init(Tpa instance) {
        tpaInstance = instance;
    }

    /**
     * 检查玩家是否合法(存在、在线否)
     * @param player 待检查玩家
     * @throws Exception 玩家检查异常
     */
    private static void checkPlayer(Player player) throws Exception {
        if (player == null) {
            throw new CommandExceptions.PlayerNotFountException();
        }
        if (!player.isOnline()) {
            throw new CommandExceptions.PlayerOfflineException();
        }
    }

    /**
     * 处理tpa和tpahere命令
     * @param sender 命令发送者
     * @param args 命令参数
     * @param tpaHere 是否为tpahere指令，是则为true，反之为false
     * @throws Exception 指令执行异常
     */
    public static void TpaRequestCommand(CommandSender sender, String[] args, boolean tpaHere) throws Exception {
        if (sender instanceof Player) {
            Player sourcePlayer = (Player) sender;
            Player targetPlayer = NewNanPlus.getPlugin().getServer().getPlayer(args[0]);

            checkPlayer(targetPlayer);
            if (sourcePlayer.equals(targetPlayer)) {
                throw new CommandExceptions.CustomCommandException(NewNanPlus.getPlugin().wolfyLanguageAPI.
                        replaceColoredKeys("$module_message.teleport.request_failed_for_sane_player$"));
            }

            tpaInstance.requestTpa(sourcePlayer, targetPlayer, tpaHere);
        }
    }

    /**
     * 处理tpaaccept和tparefuse命令
     * @param sender 命令发送者
     * @param args 命令参数
     * @param accept 是否为tpaaccept指令，是则为true，反之为false
     * @throws Exception 指令执行异常
     */
    public static void TpaResponseCommand(CommandSender sender, String[] args, boolean accept) throws Exception {
        if (sender instanceof Player) {
            Player sourcePlayer = (Player) sender;
            Player targetPlayer = NewNanPlus.getPlugin().getServer().getPlayer(UUID.fromString(args[0]));

            checkPlayer(targetPlayer);
            if (sourcePlayer.equals(targetPlayer)) {
                throw new CommandExceptions.CustomCommandException(NewNanPlus.getPlugin().wolfyLanguageAPI.
                        replaceColoredKeys("$module_message.teleport.request_failed_for_sane_player$"));
            }

            if (accept) {
                tpaInstance.acceptTpa(targetPlayer, sourcePlayer);
            } else {
                tpaInstance.refuseTpa(targetPlayer, sourcePlayer);
            }
        }
    }

    /**
     * 处理tpablockt和tpaallow命令
     * @param sender 命令发送者
     * @param args 命令参数
     * @param allow 是否为tpaallow指令，是则为true，反之为false
     * @throws Exception 指令执行异常
     */
    public static void TpaBlockListCommand(CommandSender sender, String[] args, boolean allow) throws Exception {
        if (sender instanceof Player) {
            Player sourcePlayer = (Player) sender;
            Player targetPlayer = NewNanPlus.getPlugin().getServer().getPlayer(UUID.fromString(args[0]));

            checkPlayer(targetPlayer);

            if (allow) {
                tpaInstance.removeFromBlackList(sourcePlayer, targetPlayer);
            } else {
                tpaInstance.addToBlackList(sourcePlayer, targetPlayer);
            }
        }
    }
}
