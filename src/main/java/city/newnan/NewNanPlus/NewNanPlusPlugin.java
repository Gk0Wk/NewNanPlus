package city.newnan.NewNanPlus;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Vector;

/**
 * NewNanPlus 主类
 * 插件的主类需要继承 JavaPlugin，JavaPlugin 提供了插件工作时所需要的各种方法和属性
 * 每个插件只能有一个主类，在其他地方如果需要用到这个主类，应当在实例化、传参时将这个类传过去
 */
public class NewNanPlusPlugin extends JavaPlugin {
    /**
     * Vault 经济实例
     */
    protected Economy VaultEco = null;
    /**
     * 控制台日志实例，会自动带 <code>[NewNanPlus] </code>
     */
    protected java.util.logging.Logger ConsoleLogger = null;
    /**
     * 插件的配置实例，直接和文件关联
     */
    protected FileConfiguration Config = null;

    /**
     * 正在飞行中的玩家数量，Vector是线程安全的
     */
    public Vector<Player> FlyingPlayers = new Vector<Player>();

    /**
     * 插件启用时调用的方法
     */
    @Override
    public void onEnable() {
        ConsoleLogger = getLogger();
        try {
            printINFO("§6=================================");
            printINFO("§6牛腩插件组   -   Powered by Sttot");
            printINFO("§6# 更多精彩，请见 www.newnan.city #");
            printINFO("§6=================================");
            printINFO("§a插件启动中...");
            // 绑定Vault的经济模块
            if (!bindVaultEconomy()) {
                throw new Exception("Vault 经济模块绑定失败。");
            }

            // 加载插件配置
            // 如果不存在config.yml就创建默认的，存在就不会覆盖
            saveDefaultConfig();
            Config = getConfig();
            printINFO("§a配置文件载入完毕。");

            // 初始化监听实例
            new NewNanPlusListener(this);
            printINFO("§a事件监听模块注册完毕。");

            // 注册命令
            this.getCommand("nnp").setExecutor(new NewNanPlusCommand(this));
            printINFO("§a命令模块注册完毕。");

            // 注册定时函数
            new NewNanSchedule(this).runTaskTimer(this, 0, Config.getInt("module-flyfee.tick-per-count"));
            printINFO("§a定时任务模块注册完毕。");

            printINFO("§a插件启动完毕。");
        }
        catch (Exception e) {
            // 报个错
            printERROR("§c插件启动失败：" + e.getMessage());
            // 然后跑路
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    /**
     * 插件禁用时调用的方法
     */
    @Override
    public void onDisable(){
        printINFO("§a正在保存配置文件");
        saveConfig();
    }

    /**
     * 向控制台(其实是向JAVA日志)输出INFO日志
     * @param msg 要发送的消息
     */
    public void printINFO(String msg) {
        ConsoleLogger.info(msg);
    }

    /**
     * 向控制台(其实是向JAVA日志)输出WARN日志
     * @param msg 要发送的消息
     */
    public void printWARN(String msg) {
        ConsoleLogger.warning(msg);
    }

    /**
     * 向控制台(其实是向JAVA日志)输出ERROR日志
     * @param msg 要发送的消息
     */
    public void printERROR(String msg) {
        ConsoleLogger.severe(msg);
    }

    /**
     * 给玩家发送有样式码的信息
     * @param player 玩家实例
     * @param msg 信息内容
     * @param prefix 是否带有前缀
     */
    public void sendPlayerMessage(Player player, String msg, boolean prefix) {
        if (prefix) {
            String _msg = ChatColor.translateAlternateColorCodes('&',
                    Config.getString("global-data.prefix") + msg);
            player.sendMessage(_msg);
        } else {
            String _msg = ChatColor.translateAlternateColorCodes('&', msg);
            player.sendMessage(_msg);
        }
    }

    /**
     * 给玩家发送有样式码的信息
     * @param player 玩家实例
     * @param msg 信息内容
     */
    public void sendPlayerMessage(Player player, String msg) {
        sendPlayerMessage(player, msg, true);
    }

    public static void sendPlayerActionBar(Player player, String msg) {
        String _msg = ChatColor.translateAlternateColorCodes('&', msg);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(_msg));
    }

    public void cancelFly(Player player, boolean sound) {
        FlyingPlayers.remove(player);
        player.setAllowFlight(false);
        player.setFlySpeed(1.0f);
        if (sound) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.0f);
        }
    }

    /**
     * 返回Vault的Economy实例
     * @return Economy实例
     */
    public Economy getVaultEco() {
        return VaultEco;
    }

    /**
     * 返回FileConfiguration实例
     * @return FileConfiguration实例
     */
    public FileConfiguration getConf() {
        return Config;
    }

    /**
     * 重新加载配置，如果配置文件不存在就创建，存在就读取
     */
    @Override
    public void reloadConfig() {
        // 如果不存在config.yml就创建默认的，存在就不会覆盖
        saveDefaultConfig();
        super.reloadConfig();
        Config = getConfig();
    }

    /**
     * 绑定Vault的经济模块，失败返回false
     * @return 绑定成功返回true，反之false
     */
    private boolean bindVaultEconomy() {
        // 首先检查Vault插件是否加载
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        // 再检查并获取Vault的Economy公共服务
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        // 绑定
        VaultEco = rsp.getProvider();
        // 空值检查
        return VaultEco != null;
    }
}