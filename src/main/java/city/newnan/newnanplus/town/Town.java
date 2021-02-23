package city.newnan.newnanplus.town;

import java.text.SimpleDateFormat;
import java.util.*;
import me.lucko.helper.config.ConfigurationNode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/**
 * 小镇类，记录一个小镇的各种属性。
 */
public class Town {
    /**
     * 小镇配置文件
     */
    ConfigurationNode townConfig;
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
     * 城镇信用分
     */
    public int score;
    /**
     * 小镇镇长(负责人)
     */
    public OfflinePlayer leader;

    public void saveCacheToConfig(SimpleDateFormat dateFormatter)
    {
        this.townConfig.getNode("name").setValue(this.name);
        this.townConfig.getNode("location", "world").setValue(Objects.requireNonNull(this.location.getWorld()).getName());
        this.townConfig.getNode("location", "x").setValue(this.location.getX());
        this.townConfig.getNode("location", "y").setValue(this.location.getY());
        this.townConfig.getNode("location", "z").setValue(this.location.getZ());
        this.townConfig.getNode("town-leader").setValue(this.leader.getUniqueId().toString());
        this.townConfig.getNode("intro-website").setValue(this.website);
        this.townConfig.getNode("score").setValue(this.score);
    }
}
