package city.newnan.newnanplus.utility;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * 配置文件管理器，不要用它管理config.yml
 */
public class ConfigManager {
    /**
     * 绑定的插件实例
     */
    private final Plugin plugin;

    /**
     * 配置文件缓冲
     */
    private final HashMap<String, FileConfiguration> configMap = new HashMap<>();

    /**
     * 构造函数
     * @param plugin 要绑定的插件
     */
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 检查这个资源文件是否存在，不存在就创建
     * @param configFile 资源文件路径
     */
    public void touch(String configFile) {
        // config.yml要特殊处理一下
        if (configFile.equals("config.yml")) {
            plugin.saveDefaultConfig();
            return;
        }
        // 不存在就创建
        if (!new File(plugin.getDataFolder(), configFile).exists())
            plugin.saveResource(configFile, false);
    }

    /**
     * 获取某个配置文件，如果不存在就加载默认
     * @param configFile 资源文件路径
     * @return 配置实例
     */
    public FileConfiguration get(String configFile) {
        // 已缓存则返回
        if (this.configMap.containsKey(configFile))
            return this.configMap.get(configFile);

        // 未缓存则加载
        touch(configFile);
        FileConfiguration config = null;
        // config.yml要特殊处理一下
        if (configFile.equals("config.yml")) {
            config = plugin.getConfig();
        } else {
            config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), configFile));
        }
        this.configMap.put(configFile, config);
        return config;
    }

    /**
     * 保存某个配置文件，如果之前没有加载过且磁盘中不存在，就会保存默认配置文件
     * @param configFile 资源文件路径
     * @throws IOException IO异常
     */
    public void save(String configFile) {
        // 不存在就保存默认
        if (!this.configMap.containsKey(configFile)) {
            touch(configFile);
        }
        else {
            // 存在就保存
            try {
                this.configMap.get(configFile).save(new File(plugin.getDataFolder(), configFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 放弃内存中的配置，从磁盘重新加载，如果磁盘中不存在就会加载默认配置
     * @param configFile 资源文件路径
     * @return 配置实例
     */
    public FileConfiguration reload(String configFile) {
        // 若未加载，直接加载就可以
        if (!this.configMap.containsKey(configFile)) {
            return get(configFile);
        }
        // 已加载，则需要重新加载
        touch(configFile);
        FileConfiguration config = null;
        // config.yml要特殊处理一下
        if (configFile.equals("config.yml")) {
            plugin.reloadConfig();
            config = plugin.getConfig();
        } else {
            config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), configFile));
        }
        this.configMap.put(configFile, config);
        return config;
    }

    /**
     * 重置配置文件，恢复为默认配置文件
     * @param configFile 资源文件路径
     * @return 配置实例
     */
    public FileConfiguration reset(String configFile) {
        plugin.saveResource(configFile, true);
        return reload(configFile);
    }
}
