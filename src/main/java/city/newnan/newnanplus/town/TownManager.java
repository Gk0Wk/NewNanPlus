package city.newnan.newnanplus.town;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.dynmap.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    public TownManager() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManager.get("config.yml").getBoolean("module-townmanager.enable", false)) {
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
    public void onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {

    }

    private Town _loadTown(String configPath) {
        FileConfiguration config = plugin.configManager.get(configPath);
        Town town = new Town();
        town.townConfig = config;
        town.uniqueID = UUID.fromString(Objects.requireNonNull(config.getString("uuid")));
        town.name = config.getString("name");
        town.location = new Location(
                plugin.getServer().getWorld(Objects.requireNonNull(config.getString("location.world"))),
                config.getDouble("location.x"),
                config.getDouble("location.y"),
                config.getDouble("location.z")
        );
        town.balance = config.getDouble("balance");
        town.exp = config.getInt("exp");
        town.level = config.getInt("level");
        town.leader = plugin.getServer().getOfflinePlayer(
                UUID.fromString(Objects.requireNonNull(config.getString("town-leader"))));
        town.website = config.getString("intro-website");

        ConfigurationSection resource = config.getConfigurationSection("resource");
        assert resource != null;
        resource.getKeys(false).forEach(key ->
                town.resources.put(ResourceType.fromString(key), resource.getDouble(key)));

        List<Map<?,?>> effects = config.getMapList("effect");
        effects.forEach(map -> {
            try {
                TownEffectType effect = TownEffectType.fromString((String) map.get("name"));
                Date date = plugin.dateFormatter.parse((String) map.get("expirydate"));
                town.effects.put(effect, date);
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
        });

        return town;
    }

    private void resistTown(Town town) {
        towns.put(town.uniqueID, town);
    }

    public void loadTown(UUID uuid) {
        if (towns.containsKey(uuid))
            return;
        Town town =  _loadTown((new File(plugin.getDataFolder(), "town/" + uuid.toString() + ".yml")).getPath());
        resistTown(town);
    }

    public void saveTowns() {
        towns.forEach((uuid, town) -> {
            try {
                saveTown(town);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void saveTown(Town town) throws Exception {
        town.saveCacheToConfig(plugin.dateFormatter);
        plugin.configManager.save("town/" + town.uniqueID.toString() + ".yml");
    }

    public boolean checkAndRemoveOutdatedTownEffect(Town town, TownEffectType effect) {
        if (town.effects.get(effect).before(new Date())) {
            detachEffect(town, effect);
            return true;
        } else {
            return false;
        }
    }

    public void detachEffect(Town town, TownEffectType effect) {
        if (!town.effects.containsKey(effect))
            return;
        // Unregisters Something...

        // Remove from map
        town.effects.remove(effect);
    }

    public void attachEffect(Town town, TownEffectType effect, Date date) {
        // Unregisters Something...

        // Add to map
        town.effects.put(effect, date);
    }
}
