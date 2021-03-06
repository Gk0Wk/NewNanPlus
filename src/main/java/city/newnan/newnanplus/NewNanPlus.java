package city.newnan.newnanplus;

import city.newnan.api.config.ConfigManager;
import city.newnan.api.i18n.LanguageManager;
import city.newnan.api.message.MessageManager;
import city.newnan.newnanplus.exception.CommandExceptions;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import city.newnan.newnanplus.utility.CommandManager;
import city.newnan.newnanplus.utility.PlayerConfig;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

/**
 * NewNanPlus 主类
 * 插件的主类需要继承 JavaPlugin，JavaPlugin 提供了插件工作时所需要的各种方法和属性
 * 每个插件只能有一个主类，在其他地方如果需要用到这个主类，应当在实例化、传参时将这个类传过去
 */
public class NewNanPlus extends ExtendedJavaPlugin {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private static NewNanPlus plugin = null;

    /**
     * 获取唯一实例
     * @return 唯一实例，插件加载未成功则为null
     */
    public static NewNanPlus getPlugin()
    {
        return plugin;
    }

    // 模块映射
    private final HashMap<Class<?>, NewNanPlusModule> modules = new HashMap<>();

    /**
     * 根据模块类型获取模块实例
     * @param moduleClass 模块类型
     * @return 模块实例，不存在则返回Null
     */
    public NewNanPlusModule getModule(Class<?> moduleClass)
    {
        if (modules.containsKey(moduleClass))
            return modules.get(moduleClass);
        return null;
    }

    //
    public SimpleDateFormat dateFormatter;
    public ConfigManager configManagers;
    public city.newnan.newnanplus.utility.CommandManager commandManager;
    public MessageManager messageManager;
    public LanguageManager languageManager;
    public org.anjocaido.groupmanager.GroupManager groupManager;
    public net.milkbowl.vault.economy.Economy vaultEco;
    public org.dynmap.DynmapAPI dynmapAPI;

    @Override
    protected void load()
    {
    }

    /**
     * 插件启用时调用的方法
     */
    @Override
    protected void enable()
    {
        // 核心初始化
        try {
            // 初始化配置管理器
            configManagers = new ConfigManager(this).startCleanService();
            // 初始化玩家配置管理
            PlayerConfig.init(this);
            // 日期格式化形式
            dateFormatter = new SimpleDateFormat(configManagers.get("config.yml").getNode("global-settings", "date-formatter").getString("yyyy-MM-dd HH:mm:ss"));

            // 初始化多语言模块
            languageManager = new LanguageManager(this)
                                  .register(Locale.CHINA, "lang/zh-CN.json")
                                  .setMajorLanguage(Locale.CHINA);

            // 初始化消息管理器
            messageManager = new MessageManager(this)
                                 .setLanguageProvider(languageManager)
                                 .setPlayerPrefix(languageManager.provideLanguage("$chat_prefix$"));

            // 初始化命令管理器
            commandManager = new CommandManager(this, messageManager::printf, "nnp",
                YamlConfiguration.loadConfiguration(Objects.requireNonNull(getTextResource("plugin.yml"))));
            // 错误消息初始化
            CommandExceptions.init(this);
            verbose = configManagers.get("config.yml").getNode("verbose").getBoolean(false);
        } catch (Exception e) {
            getLogger().info("§cPlugin initialize failed!");
            // 打印错误栈
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 绑定静态实例
        plugin = this;

        // 欢迎界面
        {
            messageManager.info("§b    _   __             _   __               ____  __");
            messageManager.info("§b   / | / /__ _      __/ | / /___ _____     / __ \\/ /_  _______");
            messageManager.info("§b  /  |/ / _ \\ | /| / /  |/ / __ `/ __ \\   / /_/ / / / / / ___/");
            messageManager.info("§b / /|  /  __/ |/ |/ / /|  / /_/ / / / /  / ____/ / /_/ (__  )");
            messageManager.info("§b/_/ |_/\\___/|__/|__/_/ |_/\\__,_/_/ /_/  /_/   /_/\\__,_/____/     v" + getDescription().getVersion());
            messageManager.info("§6---------------------------------------------------");
            messageManager.info("Authors:");
            getDescription().getAuthors().forEach(author -> messageManager.info("§a  - " + author));
            messageManager.info("Website: §b" + getDescription().getWebsite() + "§f    - Welcome to join us!");
            messageManager.info("§6---------------------------------------------------");
            messageManager.info("§2Loading main configure file...");
            messageManager.info("Major language: §e" + languageManager.getMajorLanguage().getLanguage().getDisplayName());
            messageManager.info("Fallback language: §e" + (languageManager.getFallbackLanguage() == null ? "" : languageManager.getFallbackLanguage().getLanguage().getDisplayName()));
        }

        // API 绑定
        try {
            messageManager.info("§6---------------------------------------------------");
            messageManager.info("§2Binding dependencies...");
            // 绑定Vault
            if (!bindVault()) {
                throw new Exception("Vault API bind failed.");
            } else {
                messageManager.info("§f[ §aO K §f] Vault API");
            }

            // 绑定GroupManager
            if (!bindGroupManager()) {
                throw new Exception("GroupManager bind failed.");
            } else {
                messageManager.info("§f[ §aO K §f] GroupManager");
            }

            // 绑定Dynmap
            if (!bindDynmapAPI()) {
                throw new Exception("Dynmap API bind failed.");
            } else {
                messageManager.info("§f[ §aO K §f] Dynmap API");
            }
        } catch (Exception e) {
            // 报个错
            messageManager.info("§cBind dependencies failed!");
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 模块注册
        {
            messageManager.info("§6---------------------------------------------------");
            messageManager.info("§2Loading modules...");
            new GlobalModule();
            loadModule(city.newnan.newnanplus.cron.Cron.class, "定时任务模块");
            loadModule(city.newnan.newnanplus.feefly.FeeFly.class, "付费飞行模块");
            loadModule(city.newnan.newnanplus.town.TownManager.class, "小镇管理模块");
            loadModule(city.newnan.newnanplus.createarea.CreateArea.class, "创造区域模块");
            loadModule(city.newnan.newnanplus.powertools.PowerTools.class, "实用工具模块");
            loadModule(city.newnan.newnanplus.railexpress.RailExpress.class, "矿车加速模块");
            loadModule(city.newnan.newnanplus.laganalyzer.LagAnalyzer.class, "卡服分析器模块");
            loadModule(city.newnan.newnanplus.playermanager.PlayerManager.class, "玩家管理模块");
            loadModule(city.newnan.newnanplus.deathtrigger.DeathTrigger.class, "死亡触发器模块");
            loadModule(city.newnan.newnanplus.dynamiceconomy.DynamicEconomy.class, "动态经济模块");
            loadModule(city.newnan.newnanplus.playermanager.MailSystem.class, "邮件系统模块");
            loadModule(city.newnan.newnanplus.teleport.Tpa.class, "TPA模块");
            messageManager.info("§6---------------------------------------------------");
        }

        messageManager.info("§aNewNanPlus is on run, have a nice day!");
        messageManager.info("");

        // 插件就绪时的执行的任务
        if (modules.containsKey(city.newnan.newnanplus.cron.Cron.class)) {
            ((city.newnan.newnanplus.cron.Cron)modules.get(city.newnan.newnanplus.cron.Cron.class)).onPluginReady();
        }
    }

    /**
     * 插件禁用时调用的方法
     */
    @Override
    protected void disable()
    {
        if (messageManager != null) {
            messageManager.info("§aDisabling NewNanPlus...");
        }

        if (modules.containsKey(city.newnan.newnanplus.cron.Cron.class)) {
            ((city.newnan.newnanplus.cron.Cron)modules.get(city.newnan.newnanplus.cron.Cron.class)).onPluginDisable();
        }

        if (modules.containsKey(city.newnan.newnanplus.dynamiceconomy.DynamicEconomy.class)) {
            ((city.newnan.newnanplus.dynamiceconomy.DynamicEconomy)
                    modules.get(city.newnan.newnanplus.dynamiceconomy.DynamicEconomy.class))
                .updateAndSave();
        }

        plugin = null;
    }

    /**
     * 加载注册一个模块
     * @param moduleClass 模块类型
     * @param moduleName 模块名称
     * @param <T> 实现NewNanPlusModule接口的类
     */
    private <T extends NewNanPlusModule> void loadModule(Class<T> moduleClass, String moduleName)
    {
        try {
            // 获取构造器并构造
            Constructor<?> constructor = moduleClass.getConstructor();
            constructor.setAccessible(true);
            NewNanPlusModule module = (NewNanPlusModule)constructor.newInstance();
            modules.put(moduleClass, module);
            // 成功则打印结果
            messageManager.info("§f[ §aO N §f] " + moduleName);
        } catch (Exception e) {
            if (e.getCause() instanceof ModuleOffException) {
                messageManager.info("§f[ §7OFF §f] " + moduleName);
            } else {
                messageManager.info("§f[§cERROR§f] " + moduleName);
                e.printStackTrace();
            }
        }
    }

    /**
     * 绑定Vault模块，失败返回false
     * @return 绑定成功返回true，反之false
     */
    private boolean bindVault()
    {
        // 首先检查Vault插件是否加载
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        // 再检查并获取Vault的Economy公共服务
        RegisteredServiceProvider<Economy> rsp1 = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp1 == null) {
            return false;
        }
        // 绑定
        vaultEco = rsp1.getProvider();
        return true;
    }

    /**
     * 绑定GroupManager模块，失败返回false
     * @return 绑定成功返回true，反之false
     */
    private boolean bindGroupManager()
    {
        final Plugin groupManager = getServer().getPluginManager().getPlugin("GroupManager");
        if (groupManager != null && groupManager.isEnabled()) {
            this.groupManager = (GroupManager)groupManager;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 绑定Dynmap API
     * @return 绑定成功则返回true，反之
     */
    private boolean bindDynmapAPI()
    {
        final Plugin dynmap = getServer().getPluginManager().getPlugin("dynmap");
        if (dynmap != null && dynmap.isEnabled()) {
            dynmapAPI = (org.dynmap.DynmapAPI)dynmap;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 重新加载插件的配置
     */
    public void reloadPluginConfig()
    {
    }

    public void printWelcome(CommandSender sender)
    {
        messageManager.printf(sender, false, "NewNanPlus v{0} 牛腩服务器专供插件", getDescription().getVersion());
        messageManager.printf(sender, false, "牛腩网站: {0}", getDescription().getWebsite());
        messageManager.printf(sender, false, "作者(欢迎一起来开发): ");
        getDescription().getAuthors().forEach(author -> messageManager.printf(sender, false, false, "  " + author));
        messageManager.printf(sender, false, "输入 /nnp help 获得更多帮助");
    }

    public void reloadModule(@NotNull CommandSender sender, @NotNull String[] args)
    {
        if (args.length == 0) {
            reloadPluginConfig();
            messageManager.printf(sender, "$global_message.plugin_reload_succeed$");
        } else {
            modules.forEach((moduleClass, module) -> {
                if (moduleClass.getSimpleName().equalsIgnoreCase(args[0])) {
                    module.reloadConfig();
                    messageManager.printf(sender, "$global_message.module_reload_succeed$", args[0]);
                }
            });
        }
    }

    static private boolean verbose;
    public static void debug(String formatString, Object... param)
    {
        if (verbose) {
            plugin.messageManager.info(" <" + Thread.currentThread().getStackTrace()[2].getMethodName() + "> " + MessageFormat.format(formatString, param));
        }
    }
}

class GlobalModule implements NewNanPlusModule {
    private final NewNanPlus plugin;

    public GlobalModule()
    {
        plugin = NewNanPlus.getPlugin();
        plugin.commandManager.register("", this);
        plugin.commandManager.register("reload", this);
        plugin.commandManager.register("save", this);
        plugin.commandManager.register("reloadconfig", this);
        plugin.commandManager.register("test", this);
        plugin.commandManager.register("version", this);
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
     * @param sender  发送指令者的实例
     * @param command 被执行的指令实例
     * @param token   指令的标识字符串
     * @param args    指令的参数
     */
    @Override
    public void executeCommand(@NotNull CommandSender sender, @NotNull Command command,
        @NotNull String token, @NotNull String[] args) throws Exception
    {
        switch (token) {
        case "":
            break;
        case "reload":
            plugin.reloadModule(sender, args);
            break;
        case "save":
            plugin.configManagers.saveAll();
            plugin.messageManager.printf(sender, "$global_message.save_all_config_succeed$");
            break;
        case "reloadconfig":
            plugin.configManagers.reload(args[0]);
            plugin.messageManager.printf(sender, "$global_message.save_config_succeed$", args[0]);
            break;
        case "test":
            break;
        case "version":
            plugin.printWelcome(sender);
            break;
        }
    }
}
