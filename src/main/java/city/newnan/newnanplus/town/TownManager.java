package city.newnan.newnanplus.town;

import city.newnan.newnanplus.NewNanPlusGlobal;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Mojang生成玩家UUID的办法：
// UUID.nameUUIDFromBytes(("OfflinePlayer:" + characterName).getBytes(StandardCharsets.UTF_8))

/**
 * 小镇模块指令模块
 */
public class TownManager {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal globalData;

    private final ConcurrentHashMap<UUID, Town> towns = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public TownManager(NewNanPlusGlobal globalData) {
        this.globalData = globalData;
        File townDir = new File(this.globalData.plugin.getDataFolder(), "town");
        if (!townDir.exists()) {
            townDir.mkdir();
        }
        for (File file : townDir.listFiles()) {
            Town town = _loadTown(file.getPath());
            resistTown(town);
        }
    }

    private Town _loadTown(String configPath) {
        FileConfiguration config = globalData.configManager.get(configPath);
        Town town = new Town();
        town.townConfig = config;
        town.uniqueID = UUID.fromString(config.getString("uuid"));
        town.name = config.getString("name");
        town.location = new Location(
                globalData.plugin.getServer().getWorld(config.getString("location.world")),
                config.getDouble("location.x"),
                config.getDouble("location.y"),
                config.getDouble("location.z")
        );
        town.balance = config.getDouble("balance");
        town.exp = config.getInt("exp");
        town.level = config.getInt("level");
        town.leader = globalData.plugin.getServer().getOfflinePlayer(UUID.fromString(config.getString("town-leader")));
        town.website = config.getString("intro-website");

        ConfigurationSection resource = config.getConfigurationSection("resource");
        resource.getKeys(false).forEach(key -> {
            town.resources.put(ResourceType.fromString(key), resource.getDouble(key));
        });

        List<Map<?,?>> effects = config.getMapList("effect");
        effects.forEach(map -> {
            try {
                TownEffectType effect = TownEffectType.fromString((String) map.get("name"));
                Date date = globalData.dateFormatter.parse((String) map.get("expirydate"));
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
        Town town =  _loadTown((new File(globalData.plugin.getDataFolder(), "town/" + uuid.toString() + ".yml")).getPath());
        resistTown(town);
    }

    public void saveTowns() {
        towns.forEach((uuid, town) -> {
            saveTown(town);
        });
    }

    public void saveTown(Town town) {
        town.saveCacheToConfig(globalData.dateFormatter);
        globalData.configManager.save("town/" + town.uniqueID.toString() + ".yml");
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
        // 如果已经有了，就更新日期
        if (town.effects.containsKey(effect)) {
            town.effects.put(effect, date);
            return;
        }
        // Unregisters Something...

        // Add to map
        town.effects.put(effect, date);
    }
}
