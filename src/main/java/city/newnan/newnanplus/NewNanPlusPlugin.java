package city.newnan.newnanplus;

import city.newnan.newnanplus.createarea.CreateArea;
import city.newnan.newnanplus.cron.Cron;
import city.newnan.newnanplus.deathtrigger.DeathTrigger;
import city.newnan.newnanplus.feefly.FeeFly;
import city.newnan.newnanplus.laganalyzer.LagAnalyzer;
import city.newnan.newnanplus.playermanager.PlayerManager;
import city.newnan.newnanplus.utility.CommandManager;
import city.newnan.newnanplus.utility.ConfigManager;
import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.api.language.Language;
import me.wolfyscript.utilities.api.language.LanguageAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

/**
 * NewNanPlus 主类
 * 插件的主类需要继承 JavaPlugin，JavaPlugin 提供了插件工作时所需要的各种方法和属性
 * 每个插件只能有一个主类，在其他地方如果需要用到这个主类，应当在实例化、传参时将这个类传过去
 */
public class NewNanPlusPlugin extends JavaPlugin {
    /**
     * 持久化存储和访问全局数据
     */
    private NewNanPlusGlobal globalData;

    /**
     * 插件启用时调用的方法
     */
    @Override
    public void onEnable() {
        // 核心初始化
        try {
            // 初始化全局存储对象
            globalData = new NewNanPlusGlobal(this);
            // 初始化配置管理器
            globalData.configManager = new ConfigManager(this);
            // 初始化WolfyAPI
            globalData.wolfyAPI = bindWolfyUtilities();
            globalData.wolfyLanguageAPI = globalData.wolfyAPI.getLanguageAPI();
            globalData.wolfyInventoryAPI = globalData.wolfyAPI.getInventoryAPI();
            // 加载配置
            globalData.reloadConfig();
            // 初始化命令管理器
            globalData.commandManager = new CommandManager(this, globalData, "nnp",
                    YamlConfiguration.loadConfiguration(Objects.requireNonNull(getTextResource("plugin.yml"))));
        }
        catch (Exception e) {
            // 打印错误栈
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
        }

        globalData.printINFO("§6=================================");
        globalData.printINFO("§6牛腩插件组   -   Powered by Sttot");
        globalData.printINFO("§6Version: " + getDescription().getVersion());
        globalData.printINFO("§6# 更多精彩，请见 www.newnan.city #");
        globalData.printINFO("§6=================================");
        globalData.printINFO("§a插件启动中...");

        // 模块初始化
        try {
            // 绑定Vault
            if (!bindVault()) {
                throw new Exception("Vault绑定失败。");
            }

            // 绑定Dynmap
            if (!bindDynmapAPI()) {
                throw new Exception("Dynmap绑定失败。");
            }

            // 飞行模块
            globalData.feeFly = new FeeFly(globalData);
            globalData.printINFO("§a付费飞行模块注册完毕。");

            // 创造区模块
            globalData.createArea = new CreateArea(globalData);
            globalData.printINFO("§a创造区模块注册完毕。");

            // 新人模块
            globalData.playerManager = new PlayerManager(globalData);
            globalData.printINFO("§a新人模块注册完毕。");

            // 死亡触发器模块
            globalData.deathTrigger = new DeathTrigger(globalData);
            globalData.printINFO("§a死亡触发器模块注册完毕。");

            // 卡服分析器模块
            globalData.lagAnalyzer = new LagAnalyzer(globalData);
            globalData.printINFO("§a卡服分析器模块注册完毕。");

            // 定时任务模块
            globalData.cron = new Cron(globalData);
            globalData.printINFO("§a定时任务模块注册完毕。");

            changeGamerules();

            globalData.printINFO("§a插件启动完毕。");
        }
        catch (Exception e) {
            // 报个错
            globalData.printERROR("§c插件启动失败：");
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
        }

        globalData.cron.onPluginReady();
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

        // 再检查并获取Vault的Permission公共服务
        RegisteredServiceProvider<Permission> rsp2 = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp2 == null) {
            return false;
        }
        // 绑定
        this.globalData.vaultPerm = rsp2.getProvider();

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
    private void changeWorldsGamerules(List<String> worlds, GameRule rule, boolean value) {
        for (String world : worlds) {
            Objects.requireNonNull(this.getServer().getWorld(world)).setGameRule(rule, value);
        }
    }
}