package city.newnan.newnanplus;

import city.newnan.newnanplus.exception.CommandExceptions;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import city.newnan.newnanplus.utility.CommandManager;
import city.newnan.newnanplus.utility.ConfigManager;
import city.newnan.newnanplus.utility.MessageManager;
import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.api.language.Language;
import net.milkbowl.vault.economy.Economy;
import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Objects;

/**
 * NewNanPlus 主类
 * 插件的主类需要继承 JavaPlugin，JavaPlugin 提供了插件工作时所需要的各种方法和属性
 * 每个插件只能有一个主类，在其他地方如果需要用到这个主类，应当在实例化、传参时将这个类传过去
 */
public class NewNanPlus extends JavaPlugin {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private static NewNanPlus plugin = null;

    /**
     * 获取唯一实例
     * @return 唯一实例，插件加载未成功则为null
     */
    public static NewNanPlus getPlugin() {
        return plugin;
    }

    // 模块映射
    private final HashMap<Class<?>, NewNanPlusModule> modules = new HashMap<>();

    /**
     * 根据模块类型获取模块实例
     * @param moduleClass 模块类型
     * @return 模块实例，不存在则返回Null
     */
    public NewNanPlusModule getModule(Class<?> moduleClass) {
        if (modules.containsKey(moduleClass))
            return modules.get(moduleClass);
        return null;
    }

    //
    public SimpleDateFormat dateFormatter;
    public city.newnan.newnanplus.utility.ConfigManager configManager;
    public city.newnan.newnanplus.utility.CommandManager commandManager;
    public city.newnan.newnanplus.utility.MessageManager messageManager;
    public me.wolfyscript.utilities.api.WolfyUtilities wolfyAPI;
    public me.wolfyscript.utilities.api.language.LanguageAPI wolfyLanguageAPI;
    public org.anjocaido.groupmanager.GroupManager groupManager;
    public net.milkbowl.vault.economy.Economy vaultEco;
    public org.dynmap.DynmapAPI dynmapAPI;

    /**
     * 插件启用时调用的方法
     */
    @Override
    public void onEnable() {
        // 核心初始化
        try {
            // 初始化配置管理器
            configManager = new ConfigManager(this);
            // 日期格式化形式
            dateFormatter = new SimpleDateFormat(
                    Objects.requireNonNull(configManager.
                    get("config.yml").
                            getString("global-settings.date-formatter", "yyyy-MM-dd HH:mm:ss")));
            // 初始化WolfyAPI
            bindWolfyUtilities();
            // globalData.wolfyInventoryAPI = globalData.wolfyAPI.getInventoryAPI();
            // 初始化消息管理器
            messageManager = new MessageManager(getLogger(),  wolfyLanguageAPI.replaceColoredKeys("$chat_prefix$"));
            // 初始化命令管理器
            commandManager = new CommandManager(this, messageManager, "nnp",
                    YamlConfiguration.loadConfiguration(Objects.requireNonNull(getTextResource("plugin.yml"))));
            // 错误消息初始化
            CommandExceptions.init(this);
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
            messageManager.printINFO("§b    _   __             _   __               ____  __");
            messageManager.printINFO("§b   / | / /__ _      __/ | / /___ _____     / __ \\/ /_  _______");
            messageManager.printINFO("§b  /  |/ / _ \\ | /| / /  |/ / __ `/ __ \\   / /_/ / / / / / ___/");
            messageManager.printINFO("§b / /|  /  __/ |/ |/ / /|  / /_/ / / / /  / ____/ / /_/ (__  )");
            messageManager.printINFO("§b/_/ |_/\\___/|__/|__/_/ |_/\\__,_/_/ /_/  /_/   /_/\\__,_/____/     v" +
                    getDescription().getVersion());
            messageManager.printINFO("§6---------------------------------------------------");
            messageManager.printINFO("Authors:");
            getDescription().getAuthors().forEach(author -> messageManager.printINFO("§a  - " + author));
            messageManager.printINFO("Website: §b" + getDescription().getWebsite() + "§f    - Welcome to join us!");
            messageManager.printINFO("§6---------------------------------------------------");
            messageManager.printINFO("§2Loading main configure file...");
            messageManager.printINFO("Major language: §e" + wolfyLanguageAPI.getActiveLanguage().getName());
            messageManager.printINFO("Fallback language: §e" + wolfyLanguageAPI.getFallbackLanguage().getName());
        }

        // API 绑定
        try {
            messageManager.printINFO("§6---------------------------------------------------");
            messageManager.printINFO("§2Binding dependencies...");
            // 绑定Vault
            if (!bindVault()) {
                throw new Exception("Vault API bind failed.");
            } else {
                messageManager.printINFO("§f[ §aO K §f] Vault API");
            }

            // 绑定GroupManager
            if (!bindGroupManager()) {
                throw new Exception("GroupManager bind failed.");
            } else {
                messageManager.printINFO("§f[ §aO K §f] GroupManager");
            }

            // 绑定Dynmap
            if (!bindDynmapAPI()) {
                throw new Exception("Dynmap API bind failed.");
            } else {
                messageManager.printINFO("§f[ §aO K §f] Dynmap API");
            }
        } catch (Exception e) {
            // 报个错
            messageManager.printINFO("§cBind dependencies failed!");
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 模块注册
        {
            messageManager.printINFO("§6---------------------------------------------------");
            messageManager.printINFO("§2Loading modules...");
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
            messageManager.printINFO("§6---------------------------------------------------");
        }

        messageManager.printINFO("§aNewNanPlus is on run, have a nice day!");
        messageManager.printINFO("");

        // 插件就绪时的执行的任务
        if (modules.containsKey(city.newnan.newnanplus.cron.Cron.class)) {
            ((city.newnan.newnanplus.cron.Cron) modules.get(city.newnan.newnanplus.cron.Cron.class)).onPluginReady();
        }
    }

    /**
     * 插件禁用时调用的方法
     */
    @Override
    public void onDisable(){
        if (messageManager != null) {
            messageManager.printINFO("§aSaving configuration files...");
        }

        if (modules.containsKey(city.newnan.newnanplus.cron.Cron.class)) {
            ((city.newnan.newnanplus.cron.Cron) modules.get(city.newnan.newnanplus.cron.Cron.class)).onPluginDisable();
        }

        if (modules.containsKey(city.newnan.newnanplus.dynamiceconomy.DynamicEconomy.class)) {
            ((city.newnan.newnanplus.dynamiceconomy.DynamicEconomy)
                    modules.get(city.newnan.newnanplus.dynamiceconomy.DynamicEconomy.class)).updateAndSave();
        }

        if (configManager != null) {
            configManager.saveAll();
        }
        plugin = null;
    }

    /**
     * 加载模块
     * @param moduleClass 模块类型
     */
    private void loadModule(Class<?> moduleClass, String moduleName) {
        try {
            // 检测接口实现情况
            if (NewNanPlusModule.class.isAssignableFrom(moduleClass)) {
                // 获取构造器并构造
                Constructor<?> constructor = moduleClass.getConstructor();
                constructor.setAccessible(true);
                NewNanPlusModule module = (NewNanPlusModule) constructor.newInstance();
                modules.put(moduleClass, module);
                // 成功则打印结果
                messageManager.printINFO("§f[ §aO N §f] " + moduleName);
            } else {
                throw new Exception("Illegal module！");
            }
        } catch (Exception e) {
            if (e.getCause() instanceof ModuleOffException) {
                messageManager.printINFO("§f[ §7OFF §f] " + moduleName);
            } else {
                messageManager.printINFO("§f[§cERROR§f] " + moduleName);
                e.printStackTrace();
            }
        }
    }

    /**
     * 绑定Vault模块，失败返回false
     * @return 绑定成功返回true，反之false
     */
    private boolean bindVault() {
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
    private boolean bindGroupManager() {
        final Plugin groupManager = getServer().getPluginManager().getPlugin("GroupManager");
        if (groupManager != null && groupManager.isEnabled())
        {
            this.groupManager = (GroupManager) groupManager;
        } else {
            return false;
        }
        return true;
    }

    /**
     * 绑定WolfyAPI
     * @throws Exception 各种可能遇到的异常
     */
    private void bindWolfyUtilities() throws Exception {
        // 创建API实例
        wolfyAPI = WolfyUtilities.getOrCreateAPI(this);
        // 多语言模块
        wolfyLanguageAPI = wolfyAPI.getLanguageAPI();
        wolfyLanguageAPI.unregisterLanguages();
        // 设置主要语言
        String majorLanguageName = configManager.get("config.yml").
                getString("global-settings.major-language");
        configManager.touch("lang/" + majorLanguageName + ".json");
        Language majorLanguage = new Language(this, majorLanguageName);
        wolfyLanguageAPI.registerLanguage(majorLanguage);
        // 设置次要语言
        String fallbackLanguageName = configManager.get("config.yml").
                getString("global-settings.fallback-language");
        if (fallbackLanguageName != null && !fallbackLanguageName.equals(majorLanguageName)) {
            configManager.touch("lang/" + fallbackLanguageName + ".json");
            Language fallbackLanguage = new Language(this, fallbackLanguageName);
            wolfyLanguageAPI.registerLanguage(fallbackLanguage);
            wolfyLanguageAPI.setFallbackLanguage(fallbackLanguage);
        }

        // 设置前缀
        wolfyAPI.setCHAT_PREFIX(wolfyLanguageAPI.replaceColoredKeys("$chat_prefix$"));
        wolfyAPI.setCONSOLE_PREFIX(wolfyLanguageAPI.replaceColoredKeys("$console_prefix$"));
    }

    /**
     * 绑定Dynmap API
     * @return 绑定成功则返回true，反之
     */
    private boolean bindDynmapAPI() {
        Plugin dynmap = Bukkit.getPluginManager().getPlugin("dynmap");
        assert dynmap != null;
        dynmapAPI = (org.dynmap.DynmapAPI) dynmap;
        return true;
    }

    /**
     * 重新加载插件的配置
     */
    public void reloadPluginConfig() {

    }

    public void printWelcome(CommandSender sender) {
        messageManager.sendMessage(sender, "NewNanPlus " + getDescription().getVersion() + " 牛腩服务器专供插件", false);
        messageManager.sendMessage(sender, "牛腩网站: " + getDescription().getWebsite(), false);
        messageManager.sendMessage(sender, "作者(欢迎一起来开发): ", false);
        getDescription().getAuthors().forEach(author -> messageManager.sendMessage(sender, "  " + author, false));
        messageManager.sendMessage(sender, "输入 /nnp help 获得更多帮助", false);
    }

    public void reloadModule(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            reloadPluginConfig();
            messageManager.sendMessage(sender, "插件重载完毕。");
        }
        else {
            modules.forEach((moduleClass, module) -> {
                if (moduleClass.getSimpleName().equals(args[0])) {
                    module.reloadConfig();
                    messageManager.sendMessage(sender, "模块 " + args[0] + " 重载完毕。");
                }
            });
        }
    }
}

class GlobalModule implements NewNanPlusModule {

    public GlobalModule() {
        NewNanPlus.getPlugin().commandManager.register("", this);
        NewNanPlus.getPlugin().commandManager.register("reload", this);
        NewNanPlus.getPlugin().commandManager.register("save", this);
        NewNanPlus.getPlugin().commandManager.register("reloadconfig", this);
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {

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
                               @NotNull String token, @NotNull String[] args) throws Exception {
        if (token.isEmpty())
            NewNanPlus.getPlugin().printWelcome(sender);
        else if (token.equals("reload"))
            NewNanPlus.getPlugin().reloadModule(sender, args);
        else if (token.equals("save")) {
            NewNanPlus.getPlugin().configManager.saveAll();
            NewNanPlus.getPlugin().messageManager.sendMessage(sender, "配置保存完毕。");
        }
        else if (token.equals("reloadconfig")) {
            NewNanPlus.getPlugin().configManager.reload(args[0]);
            NewNanPlus.getPlugin().messageManager.sendMessage(
                    sender, MessageFormat.format("配置文件{0}保存完毕。", args[0]));
        }
    }
}