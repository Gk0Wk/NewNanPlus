package city.newnan.newnanplus.utility;

import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 直接分担掉plugin.yml的功能
 */
public class CommandManager implements CommandExecutor {
    private final JavaPlugin plugin;
    private final MessageManager messageManager;
    private final String prefix;
    private final String noPermissionMessage;
    private final String consoleNotAllowMessage;
    private final String noSuchCommandMessage;
    private final String badUsageMessage;
    private final String onlyConsoleMessage;
    private final String playerOfflineMessage;
    private final String commandExecuteErrorMessage;
    private final ConfigurationSection commandsConfig;
    private final HashMap<String, CommandContainer> commandContainerHashMap = new HashMap<>();
    private final HashMap<String, CommandContainer> aliasCommandContainerHashMap = new HashMap<>();

    public CommandManager(JavaPlugin plugin, MessageManager messageManager,  String prefix, FileConfiguration config,
                          String noPermissionMessage, String consoleNotAllowMessage, String onlyConsoleMessage,
                          String noSuchCommandMessage, String badUsageMessage, String playerOfflineMessage,
                          String commandExecuteErrorMessage) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.prefix = prefix;
        this.noPermissionMessage = noPermissionMessage;
        this.consoleNotAllowMessage = consoleNotAllowMessage;
        this.noSuchCommandMessage = noSuchCommandMessage;
        this.badUsageMessage = badUsageMessage;
        this.onlyConsoleMessage = onlyConsoleMessage;
        this.playerOfflineMessage = playerOfflineMessage;
        this.commandExecuteErrorMessage = commandExecuteErrorMessage;
        this.commandsConfig = config.getConfigurationSection("commands");

        Objects.requireNonNull(plugin.getCommand(prefix)).setExecutor(this);
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

        List<String> aliases;
        if (section.isList("aliases")) {
            aliases = section.getStringList("aliases");
        } else {
            aliases = new ArrayList<>();
            aliases.add(section.getString("aliases"));
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
            // 获取token
            token = (args.length == 0) ? "" : args[0];
            // 寻找对应的指令
            container = commandContainerHashMap.get(token);
            isAlias = true;
        } else {
            container = aliasCommandContainerHashMap.get(label);
            token = container.token;
            isAlias = false;
        }

        // 如果没有找到指令
        if (container == null) {
            messageManager.sendMessage(sender, noSuchCommandMessage);
            return false;
        }
        // 检查权限
        if (sender instanceof ConsoleCommandSender) {
            if (!container.consoleAllowable) {
                messageManager.sendMessage(sender, consoleNotAllowMessage);
                return false;
            }
        }
        else if (container.permission != null && !sender.hasPermission(container.permission)) {
            messageManager.sendMessage(sender,
                    (container.permissionMessage == null) ? noPermissionMessage : container.permissionMessage);
            return false;
        }

        String[] newArgs = (args.length == 0 || isAlias) ? args : new String[args.length - 1];
        if (args.length >= 1) System.arraycopy(args, 1, newArgs, 0, args.length - 1);

        try {
            container.module.onCommand(sender, command, token, newArgs);
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
            else e.printStackTrace();
        }

        return true;
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