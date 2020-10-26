package city.newnan.newnanplus.utility;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class MessageManager {
    public MessageManager(java.util.logging.Logger consoleLogger, String prefixString) {
        this.consoleLogger = consoleLogger;
        this.prefixString = prefixString;
    }

    /**
     * 控制台日志实例，会自动带 <code>[NewNanPlus] </code>
     */
    public final java.util.logging.Logger consoleLogger;
    protected String prefixString;

    /**
     * 向控制台(其实是向JAVA日志)输出INFO日志
     * @param msg 要发送的消息
     */
    public void printINFO(String msg) {
        consoleLogger.info(msg);
    }

    /**
     * 向控制台(其实是向JAVA日志)输出WARN日志
     * @param msg 要发送的消息
     */
    public void printWARN(String msg) {
        consoleLogger.warning(msg);
    }

    /**
     * 向控制台(其实是向JAVA日志)输出ERROR日志
     * @param msg 要发送的消息
     */
    public void printERROR(String msg) {
        consoleLogger.severe(msg);
    }
    /**
     * 给玩家发送有样式码的信息
     * @param player 玩家实例
     * @param msg 信息内容
     * @param prefix 是否带有前缀
     */
    public void sendPlayerMessage(Player player, String msg, boolean prefix) {
        String _msg = prefix ?
                ChatColor.translateAlternateColorCodes('&', prefixString + msg) :
                ChatColor.translateAlternateColorCodes('&', msg);
        player.sendMessage(_msg);
    }

    /**
     * 给玩家发送有样式码的信息
     * @param player 玩家实例
     * @param msg 信息内容
     */
    public void sendPlayerMessage(Player player, String msg) {
        sendPlayerMessage(player, msg, true);
    }

    /**
     * 在某个玩家的ActionBar上显示消息
     * @param player 要显示的玩家
     * @param msg 要显示的信息
     */
    public void sendPlayerActionBar(Player player, String msg) {
        String _msg = ChatColor.translateAlternateColorCodes('&', msg);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(_msg));
    }

    /**
     * 给对象发送一条消息
     * @param sender 发送的对象
     * @param msg 发送的消息
     */
    public void sendMessage(CommandSender sender, String msg) {
        if (sender instanceof Player) {
            sendPlayerMessage((Player) sender, msg);
        } else if (sender instanceof ConsoleCommandSender) {
            printINFO(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }

    /**
     * 给对象发送一条消息
     * @param sender 发送的对象
     * @param msg 发送的消息
     * @param prefix 是否包括前缀(只对玩家有效)
     */
    public void sendMessage(CommandSender sender, String msg, boolean prefix) {
        if (sender instanceof Player) {
            sendPlayerMessage((Player) sender, msg, prefix);
        } else if (sender instanceof ConsoleCommandSender) {
            printINFO(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }
}
