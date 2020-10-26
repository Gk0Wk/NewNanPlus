package city.newnan.newnanplus.utility;

import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.*;

/**
 * 直接分担掉plugin.yml的功能
 */
public class CommandManager implements CommandExecutor {
    private final JavaPlugin plugin;
    private final MessageManager messageManager;
    private final String prefix;
    private final ConfigurationSection commandsConfig;
    private final HashMap<String, CommandContainer> commandContainerHashMap = new HashMap<>();
    private final HashMap<String, CommandContainer> aliasCommandContainerHashMap = new HashMap<>();

    public CommandManager(JavaPlugin plugin, MessageManager messageManager,  String prefix, FileConfiguration config) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.prefix = prefix;
        this.commandsConfig = config.getConfigurationSection("commands");

        Objects.requireNonNull(plugin.getCommand(prefix)).setExecutor(this);
        register("help", null);
    }

    public void register(String token, NewNanPlusModule module) {
        ConfigurationSection section = commandsConfig.getConfigurationSection(
                token.isEmpty() ? (prefix) : prefix + " " + token);
        assert section != null;

        String description = section.getString("description");
        assert description != null;
        String permissionMessage = section.getString("permission-message");
        String usage = section.getString("usage");
        assert usage != null;
        boolean console = section.getBoolean("console", true);
        boolean hidden = section.getBoolean("hidden", false);
        String permission = section.getString("permission-node");

        List<String> aliases = null;
        if (section.isList("aliases")) {
            aliases = section.getStringList("aliases");
        } else {
            aliases = new ArrayList<>();
            String alias = section.getString("aliases");
            if (alias != null)
                aliases.add(alias);
        }

        CommandContainer container = new CommandContainer(token, permission, usage, description,
                permissionMessage, hidden, console, aliases.toArray(new String[0]), module);

        aliases.forEach(alias -> {
            Objects.requireNonNull(plugin.getCommand(alias)).setExecutor(this);
            aliasCommandContainerHashMap.put(alias, container);
        });

        commandContainerHashMap.put(token, container);
    }

    public void unregister(String token) {
        CommandContainer container = commandContainerHashMap.remove(token);
        if (container != null) {
            for (String alias : container.aliases) {
                aliasCommandContainerHashMap.remove(alias);
                // 好像没有办法直接注销一个命令，unregister又不敢乱用，先放着吧
            }
        }
    }

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String token;
        CommandContainer container;
        boolean isAlias;

        //是否是别名
        if (label.equals(prefix)) {
            // 不是别名
            isAlias = false;
            // 获取token
            token = (args.length == 0) ? "" : args[0];
            // 寻找对应的指令
            container = commandContainerHashMap.get(token);
        } else {
            // 是别名
            isAlias = true;
            container = aliasCommandContainerHashMap.get(label);
            token = container.token;
        }

        if (token.equals("help")) {
            printCommandHelp(sender);
            return true;
        }

        // 如果没有找到指令
        if (container == null) {
            messageManager.sendMessage(sender, NoSuchCommandException.message);
            return false;
        }
        // 检查权限
        if (sender instanceof ConsoleCommandSender) {
            if (!container.consoleAllowable) {
                messageManager.sendMessage(sender, RefuseConsoleException.message);
                return false;
            }
        }
        else if (container.permission != null && !sender.hasPermission(container.permission)) {
            messageManager.sendMessage(sender, (container.permissionMessage == null) ?
                    NoPermissionException.message : container.permissionMessage);
            return false;
        }

        String[] newArgs = (args.length == 0 || isAlias) ? args : new String[args.length - 1];
        if (args.length >= 1) System.arraycopy(args, 1, newArgs, 0, args.length - 1);

        try {
            container.module.executeCommand(sender, command, token, newArgs);
        }
        catch (Exception e) {
            if (e instanceof NoPermissionException)
                messageManager.sendMessage(sender, NoPermissionException.message);
            else if (e instanceof BadUsageException)
                messageManager.sendMessage(sender,
                        MessageFormat.format(BadUsageException.message, container.usageSuggestion));
            else if (e instanceof NoSuchCommandException)
                messageManager.sendMessage(sender, NoSuchCommandException.message);
            else if (e instanceof OnlyConsoleException)
                messageManager.sendMessage(sender, OnlyConsoleException.message);
            else if (e instanceof PlayerOfflineException)
                messageManager.sendMessage(sender, PlayerOfflineException.message);
            else if (e instanceof PlayerNotFountException)
                messageManager.sendMessage(sender, PlayerNotFountException.message);
            else if (e instanceof PlayerMoreThanOneException)
                messageManager.sendMessage(sender, PlayerMoreThanOneException.message);
            else if (e instanceof RefuseConsoleException)
                messageManager.sendMessage(sender, RefuseConsoleException.message);
            else if (e instanceof CustomCommandException)
                messageManager.sendMessage(sender, MessageFormat.format(CustomCommandException.message,
                        ((CustomCommandException) e).reason));
            else if (e instanceof  AccessFileErrorException)
                messageManager.sendMessage(sender, MessageFormat.format(AccessFileErrorException.message,
                        ((AccessFileErrorException)e).who));
            else e.printStackTrace();
        }

        return true;
    }

    public void printCommandHelp(CommandSender sender) {
        messageManager.sendMessage(sender, "NewNanPlus Commands:", false);
        commandContainerHashMap.forEach((token, command) -> {
            if (command.hidden)
                return;
            if (!command.consoleAllowable && sender instanceof ConsoleCommandSender)
                return;
            if (command.permission != null && sender instanceof Player && !sender.hasPermission(command.permission))
                return;
            messageManager.sendMessage(sender, "/nnp " + command.token + " " + command.description, false);
            messageManager.sendMessage(sender, "  Usage: " + command.usageSuggestion, false);
            StringBuilder aliasBuffer = new StringBuilder();
            Arrays.stream(command.aliases).forEach(alias -> aliasBuffer.append(alias).append(' '));
            if (aliasBuffer.length() > 0)
                messageManager.sendMessage(sender, "  Alias: " + aliasBuffer.toString(), false);
        });
    }
}

/**
 * 同时也可以解析plugin.yml的内容
 */
class CommandContainer {
    public final String token;
    public final String permission;
    public final String usageSuggestion;
    public final String permissionMessage;
    public final String description;
    public final String[] aliases;
    public final boolean hidden;
    public final boolean consoleAllowable;
    public final NewNanPlusModule module;

    public CommandContainer(@NotNull String token, @Nullable String permission, @NotNull String usageSuggestion,
                            @NotNull String description, @Nullable String permissionMessage,  boolean hidden,
                            boolean consoleAllowable, @Nullable String[] aliases, @Nullable NewNanPlusModule module) {
        this.token = token;
        this.permission = permission;
        this.usageSuggestion = usageSuggestion;
        this.permissionMessage = permissionMessage;
        this.description = description;
        this.aliases = aliases;
        this.hidden = hidden;
        this.consoleAllowable = consoleAllowable;
        this.module = module;
    }
}