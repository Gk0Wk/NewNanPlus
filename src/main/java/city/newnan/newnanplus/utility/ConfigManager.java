package city.newnan.newnanplus.utility;

import city.newnan.newnanplus.exception.CommandExceptions;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.HashMap;
import java.util.Objects;

/**
 * 配置文件管理器，管理包括config.yml、plugin.yml等在内的配置文件
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
        assert get("config.yml") != null;
    }

    /**
     * 检查这个资源文件是否存在，不存在就创建
     * @param configFile 资源文件路径
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     */
    public boolean touch(String configFile) {
        // 不存在就创建
        if (!new File(plugin.getDataFolder(), configFile).exists()) {
            // config.yml要特殊处理一下
            if (configFile.equals("config.yml"))
                plugin.saveDefaultConfig();
            else
                plugin.saveResource(configFile, false);
            return false;
        }
        return true;
    }

    /**
     * 检查这个资源文件是否存在，如果不存在就从指定的模板复制一份
     * @param targetFile 要检查的配置文件
     * @param templateFile 模板配置文件
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     * @throws IOException IO异常
     */
    public boolean touchOrCopyTemplate(String targetFile, String templateFile) throws IOException {
        File file = new File(plugin.getDataFolder(), targetFile);
        // 如果文件不存在
        if (!file.exists()) {
            // 检查父目录
            if (!file.getParentFile().exists()) {
                boolean result = file.getParentFile().mkdirs();
            }

            // 创建文件
            boolean result = file.createNewFile();

            // 拷贝内容
            BufferedInputStream input = new BufferedInputStream(Objects.requireNonNull(plugin.getResource(templateFile)));
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
            int len;
            byte[] bytes = new byte[1024];
            while ((len=input.read(bytes)) != -1) {
                output.write(bytes, 0, len);
            }
            input.close();
            output.close();
            return false;
        }
        return true;
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
        // config.yml要特殊处理一下
        FileConfiguration config = configFile.equals("config.yml") ? plugin.getConfig() :
                YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), configFile));
        this.configMap.put(configFile, config);
        return config;
    }

    /**
     * 获取某个配置文件，如果不存在就从指定的模板复制一份
     * @param targetFile 要获取的配置文件
     * @param templateFile 模板配置文件
     * @return 配置实例
     * @throws IOException IO异常
     */
    public FileConfiguration getOrCopyTemplate(String targetFile, String templateFile) throws IOException {
        // 已缓存则返回
        if (this.configMap.containsKey(targetFile))
            return this.configMap.get(targetFile);
        // 未缓存则加载
        touchOrCopyTemplate(targetFile, templateFile);
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), targetFile));
        this.configMap.put(targetFile, config);
        return config;
    }

    /**
     * 保存某个配置文件，如果之前没有加载过且磁盘中不存在，就会保存默认配置文件
     * @param configFile 资源文件路径
     * @throws Exception 执行异常
     */
    public void save(String configFile) throws Exception {
        // 不存在就保存默认
        if (!this.configMap.containsKey(configFile)) {
            touch(configFile);
        }
        else {
            // 存在就保存
            try {
                if (configFile.equals("config.yml")) {
                    plugin.saveConfig();
                } else {
                    this.configMap.get(configFile).save(new File(plugin.getDataFolder(), configFile));
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new CommandExceptions.AccessFileErrorException(configFile);
            }
        }
    }

    /**
     * 卸载配置文件，并选择是否保存配置文件
     * @param configFile 配置文件名
     * @param save 是否保存内存中的配置文件信息到硬盘
     * @throws Exception 执行异常
     */
    public void unload(String configFile, boolean save) throws Exception {
        // 不能卸载 config.yml
        if (configFile.equals("config.yml"))
            return;
        FileConfiguration config = this.configMap.remove(configFile);
        if (config == null || !save)
            return;
        try {
            config.save(new File(plugin.getDataFolder(), configFile));
        } catch (IOException e) {
            e.printStackTrace();
            throw new CommandExceptions.AccessFileErrorException(configFile);
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
        FileConfiguration config;
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

    /**
     * 保存所有配置文件
     */
    public void saveAll() {
        configMap.forEach((name, config) -> {
            try {
                save(name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
