package city.newnan.newnanplus.teleport;

import city.newnan.api.config.ConfigManager;
import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions;
import city.newnan.newnanplus.exception.ModuleExeptions;
import city.newnan.newnanplus.utility.PlayerConfig;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import me.lucko.helper.config.ConfigurationNode;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class Tpa implements NewNanPlusModule {
    /**
     * 插件实例
     */
    private final NewNanPlus plugin;
    /**
     * 会话缓存实例
     */
    SessionCache sessionCache;

    /**
     * 传送邀请指令冷却时间(对方接受传送起开始冷却)
     */
    public long tpCoolDownTime;
    /**
     * 传送执行延迟
     */
    public long tpDelayTime;
    /**
     * 传送会话过期时间
     */
    public long tpOutdatedTime;

    public Tpa() throws Exception
    {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManagers.get("config.yml").getNode("module-teleport", "tpa", "enable").getBoolean(false))
            throw new ModuleExeptions.ModuleOffException();
        reloadConfig();
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
    public void reloadConfig()
    {
        try {
            ConfigurationNode section = plugin.configManagers.get("config.yml").getNode("module-teleport", "tpa");
            tpCoolDownTime = section.getNode("cooldown-time").getLong(0) * 1000;
            tpDelayTime = section.getNode("delay-time").getLong(0) * 20;
            tpOutdatedTime = section.getNode("outdated-time").getLong(0) * 1000;
        } catch (IOException | ConfigManager.UnknownConfigFileFormatException e) {
            e.printStackTrace();
        }
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
    public void executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception
    {
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

    /**
     * 拒绝传送
     * @param targetPlayer 拒绝者(被邀请人)
     * @param sourcePlayer 被拒绝者(邀请人)
     */
    public void refuseTpa(Player targetPlayer, Player sourcePlayer)
    {
        Boolean tpaHere = checkSession(targetPlayer, sourcePlayer);
        if (tpaHere == null)
            return;

        sessionCache.remove(sourcePlayer.getUniqueId());
        plugin.messageManager.printf(targetPlayer, "$module_message.teleport.tpa_refuse_send$", sourcePlayer.getName());
        plugin.messageManager.printf(sourcePlayer, "$module_message.teleport.tpa_refuse_receive$");
    }

    /**
     * 检查指定两个玩家之间是否有传送请求
     * @param targetPlayer 被邀请人
     * @param sourcePlayer 邀请人
     * @return 如果是TPAHere则返回true，反之为false，如果没有找到请求就返回null
     */
    private Boolean checkSession(final Player targetPlayer, final Player sourcePlayer)
    {
        long time = sessionCache.test(sourcePlayer.getUniqueId(), targetPlayer.getUniqueId(), "TPA");
        if (time < 0) {
            time = sessionCache.test(sourcePlayer.getUniqueId(), targetPlayer.getUniqueId(), "TPAHere");
            if (time < 0) {
                plugin.messageManager.printf(targetPlayer, "$module_message.teleport.request_notfound$");
            } else if (time == 0) {
                plugin.messageManager.printf(targetPlayer, "$module_message.teleport.request_outdated$");
            } else {
                return Boolean.TRUE;
            }
        } else if (time == 0) {
            plugin.messageManager.printf(targetPlayer, "$module_message.teleport.request_outdated$");
        } else {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * 接受传送邀请
     * @param targetPlayer 被邀请人
     * @param sourcePlayer 邀请人
     */
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
                plugin.messageManager.printf(targetPlayer, "$module_message.teleport.request_notfound$");
                return;
            } else if (time == 0) {
                plugin.messageManager.printf(targetPlayer, "$module_message.teleport.request_outdated$");
                return;
            } else {
                tpaHere = true;
            }
        } else if (time == 0) {
            plugin.messageManager.printf(targetPlayer, "$module_message.teleport.request_outdated$");
            return;
        } else {
            tpaHere = false;
        }

        plugin.messageManager.printf(targetPlayer, "$module_message.teleport.tpa_accept_send$", sourcePlayer.getName());
        plugin.messageManager.printf(sourcePlayer, "$module_message.teleport.tpa_accept_receive$");

        // 设置冷却
        if (sourcePlayer.hasPermission("newnanplus.tpa.nocooldown")) {
            sessionCache.remove(sourcePlayer.getUniqueId());
        } else {
            sessionCache.set(sourcePlayer.getUniqueId(), null, "CoolDown", tpCoolDownTime);
        }

        boolean finalTpaHere = tpaHere;
        Location target = finalTpaHere ? sourcePlayer.getLocation() : targetPlayer.getLocation();
        (new BukkitRunnable() {
            @Override
            public void run()
            {
                if (finalTpaHere) {
                    targetPlayer.teleport(target);
                } else {
                    sourcePlayer.teleport(target);
                }
            }
        }).runTaskLater(plugin, tpDelayTime);
    }

    /**
     * 发送传送邀请
     * @param sourcePlayer 邀请人
     * @param targetPlayer 被邀请人
     * @param tpaHere 是否为tpahere
     */
    public void requestTpa(Player sourcePlayer, Player targetPlayer, boolean tpaHere)
    {
        // 检查冷却
        long time = sessionCache.test(sourcePlayer.getUniqueId(), null, "CoolDown");
        if (time > 0) {
            plugin.messageManager.printf(sourcePlayer, "$module_message.teleport.cooling_down$",
                city.newnan.newnanplus.feefly.FeeFly.formatSecond((int)(time / 1000)));
            return;
        }

        // 如果在黑名单里，就不给对方发消息了
        // 但如果有权限就依然可以
        if (!isInBlackList(targetPlayer, sourcePlayer) || sourcePlayer.hasPermission("newnanplus.tpa.bypassblock")) {
            final TextComponent spaceText = new TextComponent(" ");
            TextComponent requestText = new TextComponent(plugin.messageManager.sprintf(tpaHere ?
                    "$module_message.teleport.tpahere_request$" : "$module_message.teleport.tpa_request$", sourcePlayer.getName()));
            TextComponent acceptButton = new TextComponent(plugin.messageManager.sprintf("$module_message.teleport.gui_accept_title$"));
            acceptButton.setColor(ChatColor.GREEN);
            acceptButton.setBold(Boolean.TRUE);
            acceptButton.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/nnp tpaaccept " + sourcePlayer.getUniqueId().toString()));
            acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    (new ComponentBuilder(plugin.messageManager.sprintf(tpaHere ?
                            "$module_message.teleport.gui_accept_describe_tpahere$" : "$module_message.teleport.gui_accept_describe_tpa$")).create())));

            TextComponent refuseButton = new TextComponent(plugin.messageManager.sprintf("$module_message.teleport.gui_refuse_title$"));
            refuseButton.setColor(ChatColor.RED);
            refuseButton.setBold(Boolean.TRUE);
            refuseButton.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/nnp tparefuse " + sourcePlayer.getUniqueId().toString()));
            refuseButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                (new ComponentBuilder(plugin.messageManager.sprintf("$module_message.teleport.gui_refuse_describe$"))).create()));

            TextComponent blockButton = new TextComponent(plugin.messageManager.sprintf("$module_message.teleport.gui_block_title$"));
            blockButton.setColor(ChatColor.YELLOW);
            blockButton.setBold(Boolean.TRUE);
            blockButton.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/nnp tpablock " + sourcePlayer.getUniqueId().toString()));
            blockButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                (new ComponentBuilder(plugin.messageManager.sprintf("$module_message.teleport.gui_block_describe$"))).create()));

            requestText.addExtra(spaceText);
            requestText.addExtra(acceptButton);
            requestText.addExtra(spaceText);
            requestText.addExtra(refuseButton);
            requestText.addExtra(spaceText);
            requestText.addExtra(blockButton);

            plugin.messageManager.printf(targetPlayer, false, "&7&l&m---------------------------------------------");
            targetPlayer.spigot().sendMessage(requestText);
            plugin.messageManager.printf(targetPlayer, false, "&7&l&m---------------------------------------------");

            // 创建会话
            sessionCache.set(sourcePlayer.getUniqueId(), targetPlayer.getUniqueId(), tpaHere ? "TPAHere" : "TPA", tpOutdatedTime);
        }

        // 发送反馈
        plugin.messageManager.printf(sourcePlayer, "$module_message.teleport.request_sent$");
    }

    /**
     * 检查某个玩家是不是在另一个玩家的传送请求黑名单里
     * @param hostPlayer 检查者
     * @param operatedPlayer 被检查者
     * @return 如果在黑名单就返回true，反之为false
     */
    public boolean isInBlackList(Player hostPlayer, OfflinePlayer operatedPlayer)
    {
        try {
            return PlayerConfig.getPlayerConfig(hostPlayer).getTpaBlockList().contains(operatedPlayer.getUniqueId().toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将一个玩家添加到另一个玩家的传送请求黑名单里
     * @param hostPlayer 添加者
     * @param operatedPlayer 被添加者
     */
    public void addToBlackList(Player hostPlayer, OfflinePlayer operatedPlayer)
    {
        UUID operatedPlayerUUID = operatedPlayer.getUniqueId();
        if (hostPlayer.getUniqueId().equals(operatedPlayerUUID)) {
            plugin.messageManager.printf(hostPlayer, "$module_message.teleport.block_tpa_failed_for_same_player$");
        } else {
            try {
                // 找一下本来存在的会话
                if (sessionCache.test(hostPlayer.getUniqueId(), operatedPlayerUUID, null) >= 0) {
                    sessionCache.remove(hostPlayer.getUniqueId());
                }

                PlayerConfig playerConfig = PlayerConfig.getPlayerConfig(hostPlayer);
                List<String> blockList = playerConfig.getTpaBlockList();
                if (!blockList.contains(operatedPlayerUUID.toString())) {
                    blockList.add(operatedPlayerUUID.toString());
                    playerConfig.commit();
                }
                plugin.messageManager.printf(hostPlayer, "$module_message.teleport.block_tpa_succeed$");
            } catch (Exception e) {
                plugin.messageManager.printf(hostPlayer, "$module_message.teleport.block_tpa_failed_for_file$");
            }
        }
    }

    /**
     * 将一个玩家从另一个玩家的传送请求黑名单里移除
     * @param hostPlayer 移除者
     * @param operatedPlayer 被移除者
     * @throws Exception 移除时发生的异常
     */
    public void removeFromBlackList(Player hostPlayer, OfflinePlayer operatedPlayer) throws Exception
    {
        PlayerConfig playerConfig = city.newnan.newnanplus.utility.PlayerConfig.getPlayerConfig(hostPlayer);
        playerConfig.getTpaBlockList().remove(operatedPlayer.getUniqueId().toString());
        playerConfig.commit();
        plugin.messageManager.printf(hostPlayer, "$module_message.teleport.block_tpa_remove_succeed$");
    }

    private double calculateTransportFee(Location location1, Location location2)
    {
        return 0.0;
    }
}
