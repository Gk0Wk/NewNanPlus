package city.newnan.NewNanPlus.Town;

import city.newnan.NewNanPlus.NewNanPlusGlobal;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TownCommand {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal GlobalData;

    public TownCommand(NewNanPlusGlobal globalData) {
        GlobalData = globalData;
        File townDir = new File(GlobalData.Plugin.getDataFolder(), "town");
        if (!townDir.exists()) {
            townDir.mkdir();
        }
        for (File file : townDir.listFiles()) {
            Town town = _loadTown(file.getPath());
            resistTown(town);
        }
    }

    private Town _loadTown(String ConfigPath) {
        YamlConfiguration config = GlobalData.Plugin.loadConf(ConfigPath);
        Town town = new Town();
        town.TownConfig = config;
        town.UniqueID = UUID.fromString(config.getString("uuid"));
        town.Name = config.getString("name");
        town.Location = new Location(
                GlobalData.Plugin.getServer().getWorld(config.getString("location.world")),
                config.getDouble("location.x"),
                config.getDouble("location.y"),
                config.getDouble("location.z")
        );
        town.Balance = config.getDouble("balance");
        town.Exp = config.getInt("exp");
        town.Level  = config.getInt("level");
        town.Leader = GlobalData.Plugin.getServer().getOfflinePlayer(UUID.fromString(config.getString("town-leader")));
        town.Website = config.getString("intro-website");

        ConfigurationSection resource = config.getConfigurationSection("resource");
        resource.getKeys(false).forEach(key -> {
            town.Resources.put(ResourceType.fromString(key), resource.getDouble(key));
        });

        List<Map<?,?>> effects = config.getMapList("effect");
        effects.forEach(map -> {
            try {
                TownEffectType effect = TownEffectType.fromString((String) map.get("name"));
                Date date = GlobalData.DateFormatter.parse((String) map.get("expirydate"));
                town.Effects.put(effect, date);
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
        });

        return town;
    }

    private void resistTown(Town town) {
        GlobalData.Towns.put(town.UniqueID, town);
    }

    public void loadTown(UUID uuid) {
        if (GlobalData.Towns.containsKey(uuid))
            return;
        Town town =  _loadTown((new File(GlobalData.Plugin.getDataFolder(), "town/" + uuid.toString() + ".yml")).getPath());
        resistTown(town);
    }

    public void saveTowns() {
        GlobalData.Towns.forEach((uuid, town) -> {
            saveTown(town);
        });
    }

    public void saveTown(Town town) {
        town.saveCacheToConfig(GlobalData.DateFormatter);
        GlobalData.Plugin.saveConf("town/" + town.UniqueID.toString() + ".yml", town.TownConfig);
    }

    public boolean checkANDremoveOutdatedTownEffect(Town town, TownEffectType effect) {
        if (town.Effects.get(effect).before(new Date())) {
            detachEffect(town, effect);
            return true;
        } else {
            return false;
        }
    }

    public void detachEffect(Town town, TownEffectType effect) {
        if (!town.Effects.containsKey(effect))
            return;
        // Unregists Something...

        // Remove from map
        town.Effects.remove(effect);
    }

    public void attachEffect(Town town, TownEffectType effect, Date date) {
        // 如果已经有了，就更新日期
        if (town.Effects.containsKey(effect)) {
            town.Effects.put(effect, date);
            return;
        }
        // Regists Something...

        // Add to map
        town.Effects.put(effect, date);
    }
}
