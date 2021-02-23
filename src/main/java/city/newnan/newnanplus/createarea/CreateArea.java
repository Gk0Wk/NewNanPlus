package city.newnan.newnanplus.createarea;

import city.newnan.api.config.ConfigManager;
import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions.BadUsageException;
import city.newnan.newnanplus.exception.CommandExceptions.CustomCommandException;
import city.newnan.newnanplus.exception.CommandExceptions.NoPermissionException;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import city.newnan.newnanplus.playermanager.PlayerManager;
import me.lucko.helper.config.ConfigurationNode;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Objects;

public class CreateArea implements NewNanPlusModule, Listener {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    /**
     * 创造世界实例
     */
    private World createWorld;
    /**
     * 创造区标记集对象
     */
    private MarkerSet createAreaMarkers;
    /**
     * GroupManager 创造世界Builder组
     */
    private Group builderGroup;
    /**
     * GroupManager 创造世界实例
     */
    private OverloadedWorldHolder createWorldPermissionHandler;

    /**
     * 构造函数
     */
    public CreateArea() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManagers.get("create_area.yml").getNode("enable").getBoolean(false)) {
            throw new ModuleOffException();
        }

        createAreaMarkers = plugin.dynmapAPI.getMarkerAPI().getMarkerSet("NewNanPlus.CreateArea");
        if (createAreaMarkers == null) {
            createAreaMarkers = plugin.dynmapAPI.getMarkerAPI().createMarkerSet(
                    "NewNanPlus.CreateArea", "创造区", null, false);
        }

        reloadConfig();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.commandManager.register("ctp", this);
        plugin.commandManager.register("cnew", this);
        plugin.commandManager.register("cdel", this);
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        try {
            ConfigurationNode createArea = plugin.configManagers.reload("create_area.yml");
            createWorld = plugin.getServer().getWorld(Objects.requireNonNull(createArea.getNode("world").getString()));

            createWorldPermissionHandler = plugin.groupManager.getWorldsHolder().getWorldData(createWorld.getName());
            builderGroup = createWorldPermissionHandler.getGroup(createArea.getNode("builder-group").getString());

            // 检查有没有在图中画出的创造区域
            createArea.getNode("areas").getChildrenMap().forEach((key, value) -> {
                if (key instanceof String && createAreaMarkers.findAreaMarker((String) key) == null) {
                    int x1 = value.getNode("x1").getInt();
                    int x2 = value.getNode("x2").getInt();
                    int z1 = value.getNode("z1").getInt();
                    int z2 = value.getNode("z2").getInt();
                    String name = value.getNode("name").getString();
                    // 地图上绘制区域
                    createAreaMarkers.createAreaMarker((String) key,
                            plugin.messageManager.sprintf("$module_message.create_area.title_on_dynmap$", name),
                            false, createWorld.getName(),
                            new double[]{x1, x1, x2, x2}, new double[]{z1, z2, z2, z1}, false);
                }
            });
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
    public void executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {
        if (token.equals("ctp"))
            teleportToCreateArea(sender, args);
        if (token.equals("cnew"))
            createCreateArea(sender, args);
        if (token.equals("cdel"))
            removeCreateArea(sender, args);
    }

    /**
     * /nnp ctp指令实现，将玩家传送到自己/某人的创造区域
     * @param sender 指令发送方
     * @param args 指令参数
     */
    public void teleportToCreateArea(CommandSender sender, String[] args) throws Exception {
        ConfigurationNode createAreas = plugin.configManagers.get("create_area.yml").getNode("areas");

        Player player = (Player) sender;
        // 输入了其他玩家的名字，传送到其他玩家所在的创造区
        if (args.length != 0) {
            // 检查权限
            if (!player.hasPermission("newnanplus.createarea.teleport.other")) {
                throw new NoPermissionException();
            }

            // 根据输入查找离线玩家
            OfflinePlayer _player = ((city.newnan.newnanplus.playermanager.PlayerManager)plugin.
                    getModule(city.newnan.newnanplus.playermanager.PlayerManager.class)).findOnePlayerByName(args[0]);

            // 检查对应的玩家有没有创造区
            ConfigurationNode area = createAreas.getNode(_player.getUniqueId().toString());
            if (area.getValue() == null) {
                throw new CustomCommandException(plugin.messageManager.sprintf(
                        "$module_message.create_area.player_have_no_area$"));
            }
            _teleportTo(area, player);

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                    plugin.messageManager.sprintf("$module_message.create_area.teleported_to_ones_area$", args[0])));
        } else {
            // 不带参数，传送到自己的创造区
            // 检查权限
            if (!player.hasPermission("newnanplus.createarea.teleport.self")) {
                throw new NoPermissionException();
            }

            // 看看玩家有没有创造区
            ConfigurationNode area = createAreas.getNode(player.getUniqueId().toString());
            if (area.getValue() == null) {
                throw new CustomCommandException(plugin.messageManager.sprintf(
                        "$module_message.create_area.you_have_no_area$"));
            }
            _teleportTo(area, player);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                    plugin.messageManager.sprintf("$module_message.create_area.teleported_to_ones_area$", player.getName())));
        }
    }

    private void _teleportTo(ConfigurationNode areaNode, Player player) {
        int x =  areaNode.getNode("x1").getInt();
        int z =  areaNode.getNode("z1").getInt();
        Location locate = new Location(
                createWorld,
                x,
                createWorld.getHighestBlockYAt(x, z),
                z);
        player.teleport(locate);
    }

    /**
     * /nnp cnew指令实现，创建某个玩家的创造区
     * @param args 命令参数
     */
    public void createCreateArea(CommandSender sender, String[] args) throws Exception {
        // 检查参数
        if (args.length < 5) {
            throw new BadUsageException();
        }

        OfflinePlayer _player = ((PlayerManager)plugin.getModule(PlayerManager.class)).findOnePlayerByName(args[0]);

        // 创建创造区域
        newCreateArea(args, _player);

        plugin.messageManager.printf(sender, "$module_message.create_area.create_area_succeed$", args[0]);
    }

    /**
     * 创造/更新创造区
     * @param args 命令行参数，[PlayerName] [X1] [Z1] [X2] [Z2]
     * @param player 创造区所属玩家
     */
    public void newCreateArea(String[] args, OfflinePlayer player) throws Exception {
        // 坐标解析
        int x1 = Integer.parseInt(args[1].split("\\.", 1)[0]);
        int x2 = Integer.parseInt(args[3].split("\\.", 1)[0]);
        if (x1 > x2) {
            x1 ^= x2;
            x2 ^= x1;
            x1 ^= x2;
        }
        int z1 = Integer.parseInt(args[2].split("\\.", 1)[0]);
        int z2 = Integer.parseInt(args[4].split("\\.", 1)[0]);
        if (z1 > z2) {
            z1 ^= z2;
            z2 ^= z1;
            z1 ^= z2;
        }

        ConfigurationNode createAreas = plugin.configManagers.get("create_area.yml").getNode("areas");

        // 看看玩家原来有没有创造区地图标记，有的话需要先删除标记
        AreaMarker marker = createAreaMarkers.findAreaMarker(player.getUniqueId().toString());
        if (marker != null) {
            marker.deleteMarker();
        } else {
            createWorldPermissionHandler.getUser(player.getUniqueId().toString()).setGroup(builderGroup);
        }

        // 地图上绘制区域
        createAreaMarkers.createAreaMarker(player.getUniqueId().toString(),
                plugin.messageManager.sprintf("$module_message.create_area.title_on_dynmap$", player.getName()),
                false, createWorld.getName(),
                new double[]{x1, x1, x2, x2}, new double[]{z1, z2, z2, z1}, false);

        // 存储玩家数据
        ConfigurationNode area = createAreas.getNode(player.getUniqueId().toString());
        area.getNode("name").setValue(player.getName());
        area.getNode("x1").setValue(x1);
        area.getNode("x2").setValue(x2);
        area.getNode("z1").setValue(z1);
        area.getNode("z1").setValue(z2);

        // 存储设置
        plugin.configManagers.save("create_area.yml");

        if (player.isOnline()) {
            plugin.messageManager.printf(player.getPlayer(), "$module_message.create_area.create_area_player_notice$");
        } else {
            ((city.newnan.newnanplus.playermanager.PlayerManager)plugin.
                    getModule(city.newnan.newnanplus.playermanager.PlayerManager.class)).
                    pushTask(player, plugin.messageManager.sprintf(
                            "nnp msg {0} $module_message.create_area.create_area_player_notice$", player.getName()));
        }
    }

    /**
     * /nnp cdel指令实现，删除某个玩家的创造区
     * @param args 命令参数
     */
    public void removeCreateArea(CommandSender sender, String[] args) throws Exception {
        // 检查参数
        if (args.length < 1) {
            throw new BadUsageException();
        }

        OfflinePlayer player = ((PlayerManager)plugin.getModule(PlayerManager.class)).findOnePlayerByName(args[0]);

        // 看看玩家原来有没有创造区地图标记，有的话需要先删除标记
        AreaMarker marker = createAreaMarkers.findAreaMarker(player.getUniqueId().toString());
        if (marker != null) {
            marker.deleteMarker();
            createWorldPermissionHandler.getUser(player.getUniqueId().toString()).
                    setGroup(createWorldPermissionHandler.getDefaultGroup());
        }

        // 删除配置文件对应的设置
        plugin.configManagers.get("create_area.yml").getNode("areas", player.getUniqueId().toString()).setValue(null);
        // 存储设置
        plugin.configManagers.save("create_area.yml");

        plugin.messageManager.printf(sender, "$module_message.create_area.remove_area_succeed$");

        if (player.isOnline()) {
            plugin.messageManager.printf(player.getPlayer(), "$module_message.create_area.remove_area_player_notice$");
        } else {
            ((city.newnan.newnanplus.playermanager.PlayerManager)plugin.
                    getModule(city.newnan.newnanplus.playermanager.PlayerManager.class)).
                    pushTask(player, plugin.messageManager.sprintf("nnp msg {0} $module_message.create_area.remove_area_player_notice$",
                            player.getName()));
        }
    }

    /**
     * 玩家切换世界事件监听函数
     * @param event 玩家切换世界事件实例
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (event.getPlayer().getWorld().equals(createWorld)) {
            checkArea(event.getPlayer());
        }
    }

    /**
     * 玩家登录时触发的方法
     * @param event 玩家登录事件
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getWorld().equals(createWorld)) {
            checkArea(event.getPlayer());
        }
    }

    /**
     * 看看玩家原来有没有创造区地图标记，没有就将玩家从Builder组移除
     * @param player 玩家实例
     */
    public void checkArea(Player player) {
        // 如果有特权，无需创造区也可以保留权限
        if (player.hasPermission("newnanplus.createarea.bypass"))
            return;
        if (createWorldPermissionHandler.getUser(player.getUniqueId().toString()).getGroup().equals(builderGroup)) {
            boolean exist = false;
            try {
                exist = plugin.configManagers.get("create_area.yml").getNode("areas", player.getUniqueId().toString()).getValue() != null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!exist) {
                createWorldPermissionHandler.getUser(player.getUniqueId().toString()).setGroup(
                        createWorldPermissionHandler.getDefaultGroup());

                plugin.messageManager.printf(player, "$module_message.create_area.remove_from_builder_for_no_area$");
            }
        }
    }
}
