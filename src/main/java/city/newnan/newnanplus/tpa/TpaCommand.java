package city.newnan.newnanplus.tpa;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.exception.CommandExceptions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpaCommand {
    private static Tpa tpaInstance;
    public static void init(Tpa instance) {
        tpaInstance = instance;
    }

    private static void checkPlayer(Player player) throws Exception {
        if (player == null) {
            throw new CommandExceptions.PlayerNotFountException();
        }
        if (!player.isOnline()) {
            throw new CommandExceptions.PlayerOfflineException();
        }
    }

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
