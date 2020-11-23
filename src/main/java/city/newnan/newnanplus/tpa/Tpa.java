package city.newnan.newnanplus.tpa;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions;
import city.newnan.newnanplus.exception.ModuleExeptions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

public class Tpa implements NewNanPlusModule {
    private final NewNanPlus plugin;
    SessionCache sessionCache;

    public long tpCoolDownTime;
    public long tpDelayTime;
    public long tpOutdatedTime;

    public Tpa() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManager.get("config.yml").getBoolean("module-teleport.tpa.enable", false))
            throw new ModuleExeptions.ModuleOffException();
        sessionCache = new SessionCache();
        TpaCommand.init(this);

        plugin.commandManager.register("tpa", this);
        plugin.commandManager.register("tpahere", this);
        plugin.commandManager.register("tpaaccept", this);
        plugin.commandManager.register("tparefuse", this);
        plugin.commandManager.register("tpablock", this);
        plugin.commandManager.register("tpaallow", this);
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        ConfigurationSection section = plugin.configManager.get("config.yml").getConfigurationSection("module-teleport.tpa");
        assert section != null;
        tpCoolDownTime = section.getLong("cooldown-time", 0) * 1000;
        tpDelayTime = section.getLong("delay-time", 0) * 20;
        tpOutdatedTime = section.getLong("outdated-time", 0) * 1000;
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
        if (args.length != 1) {
            throw new CommandExceptions.BadUsageException();
        }

        switch (token) {
            case "tpa":
                TpaCommand.TpaRequestCommand(sender, args, false);
                break;
            case "tpahere":
                TpaCommand.TpaRequestCommand(sender, args, true);
                break;
            case "tpaaccept":
                TpaCommand.TpaResponseCommand(sender, args, true);
                break;
            case "tparefuse":
                TpaCommand.TpaResponseCommand(sender, args, false);
                break;
            case "tpablock":
                TpaCommand.TpaBlockListCommand(sender, args, false);
                break;
            case "tpaallow":
                TpaCommand.TpaBlockListCommand(sender, args, true);
                break;
        }
    }

    public void refuseTpa(Player targetPlayer, Player sourcePlayer)
    {
        Boolean tpaHere = checkSession(targetPlayer, sourcePlayer);
        if (tpaHere == null)
            return;

        sessionCache.del(sourcePlayer.getUniqueId());
        plugin.messageManager.sendMessage(targetPlayer, MessageFormat.format(plugin.wolfyLanguageAPI.
                replaceColoredKeys("$module_message.teleport.tpa_refuse_send$"), sourcePlayer.getName()));
        plugin.messageManager.sendMessage(sourcePlayer,
                plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.teleport.tpa_refuse_receive$"));
    }

    private Boolean checkSession(final Player targetPlayer, final Player sourcePlayer) {
        long time = sessionCache.test(sourcePlayer.getUniqueId(), targetPlayer.getUniqueId(), "TPA");
        if (time < 0) {
            time = sessionCache.test(sourcePlayer.getUniqueId(), targetPlayer.getUniqueId(), "TPAHere");
            if (time < 0) {
                plugin.messageManager.sendMessage(targetPlayer, plugin.wolfyLanguageAPI.
                        replaceColoredKeys("$module_message.teleport.request_notfound$"));
            } else if (time == 0) {
                plugin.messageManager.sendMessage(targetPlayer, plugin.wolfyLanguageAPI.
                        replaceColoredKeys("$module_message.teleport.request_outdated$"));
            } else {
                return Boolean.TRUE;
            }
        } else if (time == 0) {
            plugin.messageManager.sendMessage(targetPlayer, plugin.wolfyLanguageAPI.
                    replaceColoredKeys("$module_message.teleport.request_outdated$"));
        } else {
            return Boolean.FALSE;
        }
        return null;
    }

    public void acceptTpa(final Player targetPlayer, final Player sourcePlayer)
    {
        Boolean tpaHere = checkSession(targetPlayer, sourcePlayer);
        if (tpaHere == null)
            return;

        // 检查会话
        long time = sessionCache.test(sourcePlayer.getUniqueId(), targetPlayer.getUniqueId(), "TPA");
        if (time < 0) {
            time = sessionCache.test(sourcePlayer.getUniqueId(), targetPlayer.getUniqueId(), "TPAHere");
            if (time < 0) {
                plugin.messageManager.sendMessage(targetPlayer, plugin.wolfyLanguageAPI.
                        replaceColoredKeys("$module_message.teleport.request_notfound$"));
                return;
            } else if (time == 0) {
                plugin.messageManager.sendMessage(targetPlayer, plugin.wolfyLanguageAPI.
                        replaceColoredKeys("$module_message.teleport.request_outdated$"));
                return;
            } else {
                tpaHere = true;
            }
        } else if (time == 0) {
            plugin.messageManager.sendMessage(targetPlayer, plugin.wolfyLanguageAPI.
                    replaceColoredKeys("$module_message.teleport.request_outdated$"));
            return;
        } else {
            tpaHere = false;
        }

        plugin.messageManager.sendMessage(targetPlayer, MessageFormat.format(plugin.wolfyLanguageAPI.
                replaceColoredKeys("$module_message.teleport.tpa_accept_send$"), sourcePlayer.getName()));
        plugin.messageManager.sendMessage(sourcePlayer,
                plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.teleport.tpa_accept_receive$"));

        sessionCache.set(sourcePlayer.getUniqueId(), null, "CoolDown", tpCoolDownTime);
        boolean finalTpaHere = tpaHere;
        (new BukkitRunnable() {
            public void run()
            {
                if (finalTpaHere) {
                    targetPlayer.teleport(sourcePlayer.getLocation());
                } else {
                    sourcePlayer.teleport(targetPlayer.getLocation());
                }
            }
        }).runTaskLater(plugin, tpDelayTime);
    }

    public void requestTpa(Player sourcePlayer, Player targetPlayer, boolean tpaHere)
    {
        // 检查冷却
        long time = sessionCache.test(sourcePlayer.getUniqueId(), null, "CoolDown");
        if (time > 0) {
            plugin.messageManager.sendMessage(sourcePlayer, MessageFormat.format(plugin.wolfyLanguageAPI.
                    replaceColoredKeys("$module_message.teleport.cooling_down$"),
                    city.newnan.newnanplus.feefly.FeeFly.formatSecond((int)(time / 1000))));
            return;
        }

        // 如果在黑名单里，就不给对方发消息了
        if (!isInBlackList(targetPlayer, sourcePlayer)) {
            final TextComponent spaceText = new TextComponent(" ");
            TextComponent requestText = new TextComponent(MessageFormat.format(
                    plugin.wolfyLanguageAPI.replaceColoredKeys(tpaHere ?
                            "$module_message.teleport.tpa_request$" : "$module_message.teleport.tpahere_request$"),
                    sourcePlayer.getName()));

            TextComponent acceptButton = new TextComponent(
                    plugin.wolfyLanguageAPI.replaceKeys("$module_message.teleport.gui_accept_title$"));
            acceptButton.setColor(ChatColor.GREEN);
            acceptButton.setBold(Boolean.TRUE);
            acceptButton.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, "/nnp tpaaccept " + sourcePlayer.getUniqueId().toString()));
            acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder(
                    plugin.wolfyLanguageAPI.replaceColoredKeys(tpaHere ?
                            "$module_message.teleport.gui_accept_describe_tpahere$"
                            : "$module_message.teleport.gui_accept_describe_tpa$"))).create()));

            TextComponent refuseButton = new TextComponent(
                    plugin.wolfyLanguageAPI.replaceKeys("$module_message.teleport.gui_refuse_title$"));
            refuseButton.setColor(ChatColor.RED);
            refuseButton.setBold(Boolean.TRUE);
            refuseButton.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, "/nnp tparefuse " + sourcePlayer.getUniqueId().toString()));
            refuseButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    (new ComponentBuilder(plugin.wolfyLanguageAPI.replaceColoredKeys(
                            "$module_message.teleport.gui_refuse_describe$"))).create()));

            TextComponent blockButton = new TextComponent(
                    plugin.wolfyLanguageAPI.replaceKeys("$module_message.teleport.gui_block_title$"));
            blockButton.setColor(ChatColor.YELLOW);
            blockButton.setBold(Boolean.TRUE);
            blockButton.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, "/nnp tpablock " + sourcePlayer.getUniqueId().toString()));
            blockButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    (new ComponentBuilder(plugin.wolfyLanguageAPI.replaceColoredKeys(
                            "$module_message.teleport.gui_block_describe$"))).create()));

            requestText.addExtra(spaceText);
            requestText.addExtra(acceptButton);
            requestText.addExtra(spaceText);
            requestText.addExtra(refuseButton);
            requestText.addExtra(spaceText);
            requestText.addExtra(blockButton);

            plugin.messageManager.sendMessage(targetPlayer, "&7&l&m---------------------------------------------");
            targetPlayer.spigot().sendMessage(requestText);
            plugin.messageManager.sendMessage(targetPlayer, "&7&l&m---------------------------------------------");

            // 创建会话
            sessionCache.set(sourcePlayer.getUniqueId(), targetPlayer.getUniqueId(), tpaHere ? "TPAHere" : "TPA", tpOutdatedTime);
        }

        // 发送反馈
        plugin.messageManager.sendMessage(sourcePlayer,
                plugin.wolfyLanguageAPI.replaceColoredKeys("$module_message.teleport.request_sent$"));
    }

    public boolean isInBlackList(Player hostPlayer, OfflinePlayer operatedPlayer)
    {
        List<String> blockList = plugin.configManager.get("player/" + hostPlayer.getUniqueId().toString() + ".yml")
                .getStringList("tpa-blocklist");
        return blockList.contains(operatedPlayer.getUniqueId().toString());
    }

    public void addToBlackList(Player hostPlayer, OfflinePlayer operatedPlayer)
    {
        try {
            UUID operatedPlayerUUID = operatedPlayer.getUniqueId();
            if (hostPlayer.getUniqueId().equals(operatedPlayerUUID)) {
                plugin.messageManager.sendMessage(hostPlayer, plugin.wolfyLanguageAPI.replaceColoredKeys(
                        "$module_message.teleport.block_tpa_failed_for_same_player$"));
            } else {
                // 找一下本来存在的会话
                if (sessionCache.test(hostPlayer.getUniqueId(), operatedPlayerUUID, null) >= 0) {
                    sessionCache.del(hostPlayer.getUniqueId());
                }

                String configPath = "player/" + hostPlayer.getUniqueId().toString() + ".yml";
                FileConfiguration config = plugin.configManager.get(configPath);
                List<String> blockList = config.getStringList("tpa-blocklist");
                if (!blockList.contains(operatedPlayerUUID.toString())) {
                    blockList.add(operatedPlayerUUID.toString());
                    config.set("tpa-blocklist", blockList);
                    plugin.configManager.save(configPath);
                }
                plugin.messageManager.sendMessage(hostPlayer, plugin.wolfyLanguageAPI.replaceColoredKeys(
                        "$module_message.teleport.block_tpa_succeed$"));
            }
        } catch (Exception e) {
            plugin.messageManager.sendMessage(hostPlayer, plugin.wolfyLanguageAPI.replaceColoredKeys(
                    "$module_message.teleport.block_tpa_failed_for_file$"));
        }
    }

    public void removeFromBlackList(Player hostPlayer, OfflinePlayer operatedPlayer) throws Exception {
        UUID operatedPlayerUUID = operatedPlayer.getUniqueId();

        String configPath = "player/" + hostPlayer.getUniqueId().toString() + ".yml";
        FileConfiguration config = plugin.configManager.get(configPath);
        List<String> blockList = config.getStringList("tpa-blocklist");
        if (blockList.contains(operatedPlayerUUID.toString())) {
            blockList.remove(operatedPlayerUUID.toString());
            config.set("tpa-blocklist", blockList);
            plugin.configManager.save(configPath);
        }
        plugin.messageManager.sendMessage(hostPlayer, plugin.wolfyLanguageAPI.replaceColoredKeys(
                "$module_message.teleport.block_tpa_remove_succeed$"));
    }
}
