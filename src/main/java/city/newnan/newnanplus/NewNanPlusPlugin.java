package city.newnan.newnanplus;

import city.newnan.newnanplus.createarea.CreateArea;
import city.newnan.newnanplus.cron.Cron;
import city.newnan.newnanplus.deathtrigger.DeathTrigger;
import city.newnan.newnanplus.feefly.FeeFly;
import city.newnan.newnanplus.laganalyzer.LagAnalyzer;
import city.newnan.newnanplus.playermanager.PlayerManager;
import city.newnan.newnanplus.utility.ConfigManager;
import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.api.language.Language;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;

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
        try {
            // 实例化全局存储对象 + 配置管理器
            this.globalData = new NewNanPlusGlobal(this, new ConfigManager(this));
            this.globalData.reloadConfig();
        }
        catch (Exception e) {
            // 打印错误栈
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
        }

        try {
            this.globalData.printINFO("§6=================================");
            this.globalData.printINFO("§6牛腩插件组   -   Powered by Sttot");
            this.globalData.printINFO("§6# 更多精彩，请见 www.newnan.city #");
            this.globalData.printINFO("§6=================================");
            this.globalData.printINFO("§a插件启动中...");

            // 绑定Vault的经济模块
            if (!bindVault()) {
                throw new Exception("Vault模块绑定失败。");
            }

            // 加载插件配置
            this.globalData.configManager.get("config.yml");
            this.globalData.printINFO("§a配置文件载入完毕。");

            bindWolfyUtilities();
            bindDynmapAPI();

            // 初始化监听实例
            this.globalData.listener = new NewNanPlusListener(globalData);
            this.globalData.printINFO("§a事件监听模块注册完毕。");

            // 注册命令
            this.globalData.command = new NewNanPlusCommand(globalData);
            this.globalData.printINFO("§a命令模块注册完毕。");

            // 飞行模块
            this.globalData.feeFly = new FeeFly(globalData);
            this.globalData.printINFO("§a付费飞行模块注册完毕。");

            // 创造区模块
            this.globalData.createArea = new CreateArea(globalData);
            this.globalData.printINFO("§a创造区模块注册完毕。");

            // 新人模块
            this.globalData.playerManager = new PlayerManager(globalData);
            this.globalData.printINFO("§a新人模块注册完毕。");

            // 死亡触发器模块
            this.globalData.deathTrigger = new DeathTrigger(globalData);
            this.globalData.printINFO("§a死亡触发器模块注册完毕。");

            // 卡服分析器模块
            this.globalData.lagAnalyzer = new LagAnalyzer(globalData);
            this.globalData.printINFO("§a卡服分析器模块注册完毕。");

            // 定时任务模块
            this.globalData.cron = new Cron(globalData);
            this.globalData.printINFO("§a定时任务模块注册完毕。");

            changeGamerules();

            this.globalData.printINFO("§a插件启动完毕。");
        }
        catch (Exception e) {
            // 报个错
            this.globalData.printERROR("§c插件启动失败：");
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
        if (this.globalData != null) {
            if (this.globalData.cron != null)
                this.globalData.cron.onPluginDisable();
            this.globalData.printINFO("§a正在保存配置文件");
        }
        saveConfig();
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

        // 空值检查
        return this.globalData.vaultEco != null && this.globalData.vaultPerm != null;
    }

    private boolean bindWolfyUtilities() {
        FileConfiguration config = globalData.configManager.get("config.yml");
        // 创建API实例
        this.globalData.wolfyAPI = WolfyUtilities.getOrCreateAPI(this);
        // 设置前缀
        this.globalData.wolfyAPI.setCHAT_PREFIX(globalData.globalMessage.get("PREFIX"));
        this.globalData.wolfyAPI.setCONSOLE_PREFIX("NewNanCity");
        // 多语言模块
        this.saveResource("lang/zh-CN.json", true);
        this.globalData.wolfyLanguageAPI = this.globalData.wolfyAPI.getLanguageAPI();
        try{
            this.globalData.wolfyLanguageAPI.registerLanguage(new Language(this, "zh-CN"));
        }
        catch (IOException e) {
            this.globalData.printERROR("无法打开语言文件！");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
        // 创建背包界面API实例
        this.globalData.wolfyInventoryAPI = this.globalData.wolfyAPI.getInventoryAPI();

        // demo
//        GuiCluster cluster = this.globalData.wolfyInventoryAPI.getOrRegisterGuiCluster("none");
//        GuiWindow window = new GuiWindow("main_menu", this.globalData.wolfyInventoryAPI, InventoryType.CHEST);
//        ButtonState bstate = new ButtonState("settings", Material.OAK_LOG);
//        DummyButton button = new DummyButton("settings", bstate);
//        window.registerButton(button);
//        cluster.registerGuiWindow(window);

        return true;
    }

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
        for (String rule : rules.getKeys(false)) {
            switch (rule) {
                case "keepInventoryOnDeath":
                    changeWorldsGamerules((List<String>) rules.getList(rule), GameRule.KEEP_INVENTORY, true);
                    break;
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
            this.getServer().getWorld(world).setGameRule(rule, value);
        }
    }
}