package city.newnan.NewNanPlus.Town;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 小镇类，记录一个小镇的各种属性。
 */
public class Town {
    /**
     * 小镇配置文件
     */
    YamlConfiguration TownConfig;
    /**
     * 镇统一标识
     */
    public UUID UniqueID;
    /**
     * 镇名称
     */
    public String Name;
    /**
     * 镇地址
     */
    public Location Location;
    /**
     * 小镇介绍链接
     */
    public String Website;
    /**
     * 城镇等级
     */
    public int Level;
    /**
     * 城镇经验槽
     */
    public int Exp;
    /**
     * 城镇存款
     */
    public double Balance;
    /**
     * 小镇 Buff / Debuff
     */
    public ConcurrentHashMap<TownEffectType, Date> Effects = new ConcurrentHashMap<TownEffectType, Date>();
    /**
     * 小镇资源
     */
    public ConcurrentHashMap<ResourceType, Double> Resources = new ConcurrentHashMap<ResourceType, Double>();
    /**
     * 小镇镇长(负责人)
     */
    public OfflinePlayer Leader;

    public void saveCacheToConfig(SimpleDateFormat dateFromatter) {
        this.TownConfig.set("name", this.Name);
        this.TownConfig.set("location.world", this.Location.getWorld().getName());
        this.TownConfig.set("location.x", this.Location.getX());
        this.TownConfig.set("location.y", this.Location.getY());
        this.TownConfig.set("location.z", this.Location.getZ());
        this.TownConfig.set("balance", this.Balance);
        this.TownConfig.set("exp", this.Exp);
        this.TownConfig.set("level", this.Level);
        this.TownConfig.set("town-leader", this.Leader.getUniqueId().toString());
        this.TownConfig.set("intro-website", this.Website);

        ConfigurationSection resource = this.TownConfig.getConfigurationSection("resource");
        this.Resources.forEach((resourceType, amount) -> {
            resource.set(resourceType.toString(), amount);
        });

        ArrayList<HashMap<String,String>> effects = new ArrayList<HashMap<String,String>>();
        this.Effects.forEach((townEffectType, date) -> {
            HashMap<String, String> effect = new HashMap<String, String>();
            effect.put("name", townEffectType.toString());
            effect.put("expirydate", dateFromatter.format(date));
            effects.add(effect);
        });
        resource.set("effect", effects);
    }
}
