package city.newnan.newnanplus.town;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 小镇类，记录一个小镇的各种属性。
 */
public class Town {
    /**
     * 小镇配置文件
     */
    FileConfiguration townConfig;
    /**
     * 镇统一标识
     */
    public UUID uniqueID;
    /**
     * 镇名称
     */
    public String name;
    /**
     * 镇地址
     */
    public Location location;
    /**
     * 小镇介绍链接
     */
    public String website;
    /**
     * 城镇等级
     */
    public int level;
    /**
     * 城镇经验槽
     */
    public int exp;
    /**
     * 城镇存款
     */
    public double balance;
    /**
     * 小镇 Buff / Debuff
     */
    public ConcurrentHashMap<TownEffectType, Date> effects = new ConcurrentHashMap<TownEffectType, Date>();
    /**
     * 小镇资源
     */
    public ConcurrentHashMap<ResourceType, Double> resources = new ConcurrentHashMap<ResourceType, Double>();
    /**
     * 小镇镇长(负责人)
     */
    public OfflinePlayer leader;

    public void saveCacheToConfig(SimpleDateFormat dateFromatter) {
        this.townConfig.set("name", this.name);
        this.townConfig.set("location.world", this.location.getWorld().getName());
        this.townConfig.set("location.x", this.location.getX());
        this.townConfig.set("location.y", this.location.getY());
        this.townConfig.set("location.z", this.location.getZ());
        this.townConfig.set("balance", this.balance);
        this.townConfig.set("exp", this.exp);
        this.townConfig.set("level", this.level);
        this.townConfig.set("town-leader", this.leader.getUniqueId().toString());
        this.townConfig.set("intro-website", this.website);

        ConfigurationSection resource = this.townConfig.getConfigurationSection("resource");
        this.resources.forEach((resourceType, amount) -> {
            resource.set(resourceType.toString(), amount);
        });

        ArrayList<HashMap<String,String>> effects = new ArrayList<HashMap<String,String>>();
        this.effects.forEach((townEffectType, date) -> {
            HashMap<String, String> effect = new HashMap<String, String>();
            effect.put("name", townEffectType.toString());
            effect.put("expirydate", dateFromatter.format(date));
            effects.add(effect);
        });
        resource.set("effect", effects);
    }
}
