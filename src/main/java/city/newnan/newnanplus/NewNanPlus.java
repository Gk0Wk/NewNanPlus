package city.newnan.newnanplus;

import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import city.newnan.newnanplus.utility.CommandManager;
import city.newnan.newnanplus.utility.ConfigManager;
import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.api.language.Language;
import me.wolfyscript.utilities.api.language.LanguageAPI;
import net.milkbowl.vault.economy.Economy;
import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;

/**
 * NewNanPlus 主类
 * 插件的主类需要继承 JavaPlugin，JavaPlugin 提供了插件工作时所需要的各种方法和属性
 * 每个插件只能有一个主类，在其他地方如果需要用到这个主类，应当在实例化、传参时将这个类传过去
 */
public class NewNanPlus extends JavaPlugin {
    /**
     * 持久化存储和访问全局数据
     */
    private GlobalData globalData;

    /**
     * 插件启用时调用的方法
     */
    @Override
    public void onEnable() {
        // 核心初始化
        try {
            // 初始化全局存储对象
            globalData = new GlobalData(this);
            // 初始化配置管理器
            globalData.configManager = new ConfigManager(this);
            // 初始化WolfyAPI
            globalData.wolfyAPI = bindWolfyUtilities();
            globalData.wolfyLanguageAPI = globalData.wolfyAPI.getLanguageAPI();
            // globalData.wolfyInventoryAPI = globalData.wolfyAPI.getInventoryAPI();
            // 加载配置
            globalData.reloadConfig();
            // 初始化命令管理器
            globalData.commandManager = new CommandManager(this, globalData, "nnp",
                    YamlConfiguration.loadConfiguration(Objects.requireNonNull(getTextResource("plugin.yml"))));
            // 最后的初始化
            globalData.otherInit();
        }
        catch (Exception e) {
            getLogger().info("§cPlugin initialize failed!");
            // 打印错误栈
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // 欢迎界面
        {
            globalData.printINFO("§b    _   __             _   __               ____  __");
            globalData.printINFO("§b   / | / /__ _      __/ | / /___ _____     / __ \\/ /_  _______");
            globalData.printINFO("§b  /  |/ / _ \\ | /| / /  |/ / __ `/ __ \\   / /_/ / / / / / ___/");
            globalData.printINFO("§b / /|  /  __/ |/ |/ / /|  / /_/ / / / /  / ____/ / /_/ (__  )");
            globalData.printINFO("§b/_/ |_/\\___/|__/|__/_/ |_/\\__,_/_/ /_/  /_/   /_/\\__,_/____/     v" +
                    getDescription().getVersion());
            globalData.printINFO("§6---------------------------------------------------");
            globalData.printINFO("Authors:");
            getDescription().getAuthors().forEach(author -> globalData.printINFO("§a  - " + author));
            globalData.printINFO("Website: §b" + getDescription().getWebsite() + "§f    - Welcome to join us!");
            globalData.printINFO("§6---------------------------------------------------");
            globalData.printINFO("§2Loading main configure file...");
            globalData.printINFO("Major language: §e" + globalData.wolfyLanguageAPI.getActiveLanguage().getName());
            globalData.printINFO("Fallback language: §e" + globalData.wolfyLanguageAPI.getFallbackLanguage().getName());
        }

        try {
            globalData.printINFO("§6---------------------------------------------------");
            globalData.printINFO("§2Binding dependencies...");
            // 绑定Vault
            if (!bindVault()) {
                throw new Exception("Vault API bind failed.");
            } else {
                globalData.printINFO("§f[ §aO K §f] Vault API");
            }

            // 绑定Vault
            if (!bindGroupManager()) {
                throw new Exception("GroupManager bind failed.");
            } else {
                globalData.printINFO("§f[ §aO K §f] GroupManager");
            }

            // 绑定Dynmap
            if (!bindDynmapAPI()) {
                throw new Exception("Dynmap API bind failed.");
            } else {
                globalData.printINFO("§f[ §aO K §f] Dynmap API");
            }
        } catch (Exception e) {
            // 报个错
            globalData.printINFO("§cBind dependencies failed!");
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // 模块注册
        globalData.printINFO("§6---------------------------------------------------");
        globalData.printINFO("§2Loading modules...");
        loadModule(city.newnan.newnanplus.feefly.FeeFly.class, "付费飞行模块");
        loadModule(city.newnan.newnanplus.createarea.CreateArea.class, "创造区域模块");
        loadModule(city.newnan.newnanplus.playermanager.PlayerManager.class, "玩家管理模块");
        loadModule(city.newnan.newnanplus.deathtrigger.DeathTrigger.class, "死亡触发器模块");
        loadModule(city.newnan.newnanplus.laganalyzer.LagAnalyzer.class, "卡服分析器模块");
        loadModule(city.newnan.newnanplus.cron.Cron.class, "定时任务模块");
        loadModule(city.newnan.newnanplus.town.TownManager.class, "小镇管理模块");
        globalData.printINFO("§6---------------------------------------------------");

        globalData.printINFO("§aNewNanPlus is on run, have a nice day!");
        globalData.printINFO("");

        if (globalData.cron != null)
            globalData.cron.onPluginReady();
        changeGamerules();
    }

    /**
     * 插件禁用时调用的方法
     */
    @Override
    public void onDisable(){
        if (globalData != null) {
            if (globalData.cron != null)
                globalData.cron.onPluginDisable();
            globalData.printINFO("§a正在保存配置文件...");
            if (globalData.configManager != null) {
                globalData.configManager.saveAll();
            }
        }
    }

    /**
     * 加载模块
     * @param module 模块类型
     */
    public void loadModule(Class<?> module, String moduleName) {
        try {
            // 为什么这个isAssignableFrom是反着用的呢？
            if (NewNanPlusModule.class.isAssignableFrom(module)) {
                // 获取构造器并构造
                Constructor<?> constructor = module.getDeclaredConstructor(GlobalData.class);
                constructor.setAccessible(true);
                constructor.newInstance(globalData);
                // 成功则打印结果
                globalData.printINFO("§f[ §aO N §f] " + moduleName);
            } else {
                throw new Exception("模块不合法！");
            }
        } catch (Exception e) {
            if (e.getCause() instanceof ModuleOffException) {
                globalData.printINFO("§f[ §7OFF §f] " + moduleName);
            } else {
                globalData.printINFO("§f[§cERROR§f] " + moduleName);
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
        this.globalData.vaultEco = rsp1.getProvider();
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
            globalData.groupManager = (GroupManager) groupManager;
        } else {
            return false;
        }
        return true;
    }

    /**
     * 绑定WolfyAPI
     * @return WolfyAPI实例
     * @throws Exception 各种可能遇到的异常
     */
    private WolfyUtilities bindWolfyUtilities() throws Exception {
        // 创建API实例
        WolfyUtilities wolfyAPI = WolfyUtilities.getOrCreateAPI(this);

        // 多语言模块
        LanguageAPI wolfyLanguageAPI = wolfyAPI.getLanguageAPI();
        // 设置主要语言
        String majorLanguageName = globalData.configManager.get("config.yml").
                getString("global-settings.major-language");
        globalData.configManager.touch("lang/" + majorLanguageName + ".json");
        Language majorLanguage = new Language(this, majorLanguageName);
        wolfyLanguageAPI.registerLanguage(majorLanguage);
        // 设置次要语言
        String fallbackLanguageName = globalData.configManager.get("config.yml").
                getString("global-settings.fallback-language");
        if (fallbackLanguageName != null && !fallbackLanguageName.equals(majorLanguageName)) {
            globalData.configManager.touch("lang/" + fallbackLanguageName + ".json");
            Language fallbackLanguage = new Language(this, fallbackLanguageName);
            wolfyLanguageAPI.registerLanguage(fallbackLanguage);
            wolfyLanguageAPI.setFallbackLanguage(fallbackLanguage);
        }

        // 设置前缀
        wolfyAPI.setCHAT_PREFIX(wolfyLanguageAPI.replaceKeys("$chat_prefix$"));
        wolfyAPI.setCONSOLE_PREFIX(wolfyLanguageAPI.replaceKeys("$console_prefix$"));

        // 创建背包界面API实例
        // this.globalData.wolfyInventoryAPI = this.globalData.wolfyAPI.getInventoryAPI();

        // demo
//        GuiCluster cluster = this.globalData.wolfyInventoryAPI.getOrRegisterGuiCluster("none");
//        GuiWindow window = new GuiWindow("main_menu", this.globalData.wolfyInventoryAPI, InventoryType.CHEST);
//        ButtonState bstate = new ButtonState("settings", Material.OAK_LOG);
//        DummyButton button = new DummyButton("settings", bstate);
//        window.registerButton(button);
//        cluster.registerGuiWindow(window);

        return wolfyAPI;
    }

    /**
     * 绑定Dynmap API
     * @return 绑定成功则返回true，反之
     */
    private boolean bindDynmapAPI() {
        Plugin dynmap = Bukkit.getPluginManager().getPlugin("dynmap");
        assert dynmap != null;
        globalData.dynmapAPI = (org.dynmap.DynmapAPI) dynmap;
        return true;
    }

    /**
     * 设置世界的Gamerule
     */
    public void changeGamerules() {
        ConfigurationSection rules = globalData.configManager.get("config.yml").
                getConfigurationSection("module-world-setting");
        assert rules != null;
        for (String rule : rules.getKeys(false)) {
            if ("keepInventoryOnDeath".equals(rule)) {
                changeWorldsGamerules(Objects.requireNonNull(rules.getStringList(rule)),
                        GameRule.KEEP_INVENTORY, true);
            }
        }
    }

    /**
     * 设置一组世界的某一个Gamerule
     * @param worlds 世界列表
     * @param rule 规则
     * @param value 目标值
     */
    private void changeWorldsGamerules(List<String> worlds, GameRule<Boolean> rule, boolean value) {
        for (String world : worlds) {
            Objects.requireNonNull(this.getServer().getWorld(world)).setGameRule(rule, value);
        }
    }
}