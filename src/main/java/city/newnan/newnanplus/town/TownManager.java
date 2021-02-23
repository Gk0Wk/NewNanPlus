package city.newnan.newnanplus.town;

import city.newnan.api.config.ConfigManager;
import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import me.lucko.helper.config.ConfigurationNode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.dynmap.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;

// Mojang生成玩家UUID的办法：
// UUID.nameUUIDFromBytes(("OfflinePlayer:" + characterName).getBytes(StandardCharsets.UTF_8))

/**
 * 小镇模块指令模块
 */
public class TownManager implements NewNanPlusModule {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    private final ConcurrentHashMap<UUID, Town> towns = new ConcurrentHashMap<>();

    private MarkerSet townMarkers;

    /**
     * 构造函数
     * @throws Exception 构造时异常
     */
    public TownManager() throws Exception
    {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManagers.get("config.yml").getNode("module-townmanager", "enable").getBoolean(false)) {
            throw new ModuleOffException();
        }
        File townDir = new File(plugin.getDataFolder(), "town");
        if (!townDir.exists()) {
            boolean result = townDir.mkdirs();
        }
        for (File file : Objects.requireNonNull(townDir.listFiles())) {
            Town town = _loadTown(file.getPath());
            resistTown(town);
        }

        townMarkers = plugin.dynmapAPI.getMarkerAPI().getMarkerSet("NewNanPlus.Towns");
        if (townMarkers == null) {
            townMarkers = plugin.dynmapAPI.getMarkerAPI().createMarkerSet(
                "NewNanPlus.Towns", "Towns", null, false);
        }
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig()
    {
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
    }

    private Town _loadTown(String configPath)
    {
        try {
            ConfigurationNode config = plugin.configManagers.get(configPath);
            Town town = new Town();
            town.townConfig = config;
            town.uniqueID = UUID.fromString(Objects.requireNonNull(config.getString("uuid")));
            town.name = config.getNode("name").getString();
            town.location = new Location(
                    plugin.getServer().getWorld(Objects.requireNonNull(config.getNode("location", "world").getString())),
                    config.getNode("location", "x").getDouble(),
                    config.getNode("location", "y").getDouble(),
                    config.getNode("location", "z").getDouble());
            town.score = config.getNode("level").getInt(0);
            town.leader = plugin.getServer().getOfflinePlayer(
                    UUID.fromString(Objects.requireNonNull(config.getNode("town-leader").getString())));
            town.website = config.getNode("intro-website").getString();
            return town;
        } catch (IOException | ConfigManager.UnknownConfigFileFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void resistTown(Town town)
    {
        towns.put(town.uniqueID, town);
    }

    public void loadTown(UUID uuid)
    {
        if (towns.containsKey(uuid))
            return;
        Town town = _loadTown((new File(plugin.getDataFolder(), "town/" + uuid.toString() + ".yml")).getPath());
        resistTown(town);
    }

    public void saveTowns()
    {
        towns.forEach((uuid, town) -> {
            try {
                saveTown(town);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void saveTown(Town town) throws Exception
    {
        town.saveCacheToConfig(plugin.dateFormatter);
        plugin.configManagers.save("town/" + town.uniqueID.toString() + ".yml");
    }

    public List<Town> getTowns()
    {
        ArrayList<Town> townsList = new ArrayList<>();
        towns.forEach((uuid, town) -> townsList.add(town));
        return townsList;
    }
}
