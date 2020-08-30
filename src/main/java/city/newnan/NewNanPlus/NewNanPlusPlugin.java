package city.newnan.NewNanPlus;

import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.api.inventory.GuiWindow;
import me.wolfyscript.utilities.api.inventory.button.Button;
import me.wolfyscript.utilities.api.inventory.button.ButtonAction;
import me.wolfyscript.utilities.api.inventory.button.ButtonState;
import me.wolfyscript.utilities.api.inventory.button.ButtonType;
import me.wolfyscript.utilities.api.inventory.button.buttons.ActionButton;
import me.wolfyscript.utilities.api.inventory.GuiCluster;
import me.wolfyscript.utilities.api.inventory.button.buttons.DummyButton;
import me.wolfyscript.utilities.api.language.Language;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.wolfyscript.utilities.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * NewNanPlus 主类
 * 插件的主类需要继承 JavaPlugin，JavaPlugin 提供了插件工作时所需要的各种方法和属性
 * 每个插件只能有一个主类，在其他地方如果需要用到这个主类，应当在实例化、传参时将这个类传过去
 */
public class NewNanPlusPlugin extends JavaPlugin {
    /**
     * 持久化存储和访问全局数据
     */
    private NewNanPlusGlobal GlobalData;

    /**
     * 插件启用时调用的方法
     */
    @Override
    public void onEnable() {
        try {
            // 实例化全局存储对象
            GlobalData = new NewNanPlusGlobal();
            GlobalData.Plugin = this;
            // 绑定控制台输出
            GlobalData.ConsoleLogger = getLogger();
        }
        catch (Exception e) {
            // 打印错误栈
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
        }

        try {
            GlobalData.printINFO("§6=================================");
            GlobalData.printINFO("§6牛腩插件组   -   Powered by Sttot");
            GlobalData.printINFO("§6# 更多精彩，请见 www.newnan.city #");
            GlobalData.printINFO("§6=================================");
            GlobalData.printINFO("§a插件启动中...");

            // 绑定Vault的经济模块
            if (!bindVault()) {
                throw new Exception("Vault模块绑定失败。");
            }

            // 加载插件配置
            // 如果不存在config.yml就创建默认的，存在就不会覆盖
            saveDefaultConfig();
            GlobalData.Config = getConfig();
            GlobalData.NewbiesList = loadConf("newbies_list.yml");
            GlobalData.CreateArea = loadConf("create_area.yml");
            GlobalData.printINFO("§a配置文件载入完毕。");

            bindWolfyUtilities();

            // 初始化监听实例
            GlobalData.Listener = new NewNanPlusListener(GlobalData);
            GlobalData.printINFO("§a事件监听模块注册完毕。");

            // 注册命令
            GlobalData.Command = new NewNanPlusCommand(GlobalData);
            GlobalData.printINFO("§a命令模块注册完毕。");

            // 飞行模块
            GlobalData.FlySchedule = new city.newnan.NewNanPlus.Fly.FlySchedule(GlobalData);
            GlobalData.FlyCommand = new city.newnan.NewNanPlus.Fly.FlyCommand(GlobalData);
            GlobalData.printINFO("§a付费飞行模块注册完毕。");

            // 创造区模块
            GlobalData.CACommand = new city.newnan.NewNanPlus.CreateArea.CACommand(GlobalData);
            GlobalData.printINFO("§a创造区模块注册完毕。");

            // 新人模块
            GlobalData.PlayerCommand = new city.newnan.NewNanPlus.Player.PlayerCommand(GlobalData);
            GlobalData.printINFO("§a新人模块注册完毕。");

            // 死亡触发器模块
            GlobalData.DTCommand = new city.newnan.NewNanPlus.DeathTrigger.DTCommand(GlobalData);
            GlobalData.printINFO("§a死亡触发器模块注册完毕。");

            // 卡服分析器模块
            GlobalData.LACommand = new city.newnan.NewNanPlus.LaggAnalyzer.LACommand(GlobalData);
            GlobalData.printINFO("§a卡服分析器模块注册完毕。");

            // 定时任务模块
            GlobalData.CornCommand = new city.newnan.NewNanPlus.Corn.CornCommand(GlobalData);
            GlobalData.printINFO("§a定时任务模块注册完毕。");

            changeGamerules();

            GlobalData.printINFO("§a插件启动完毕。");
        }
        catch (Exception e) {
            // 报个错
            GlobalData.printERROR("§c插件启动失败：");
            e.printStackTrace();
            // 禁用插件
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    /**
     * 插件禁用时调用的方法
     */
    @Override
    public void onDisable(){
        if (GlobalData != null) {
            GlobalData.CornCommand.runOnPluginDisable();
            GlobalData.printINFO("§a正在保存配置文件");
        }
        saveConfig();
    }

    /**
     * 重新加载配置，如果配置文件不存在就创建，存在就读取
     */
    @Override
    public void reloadConfig() {
        // 如果不存在config.yml就创建默认的，存在就不会覆盖
        saveDefaultConfig();
        super.reloadConfig();
        GlobalData.Config = getConfig();
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
        GlobalData.VaultEco = rsp1.getProvider();

        // 再检查并获取Vault的Permission公共服务
        RegisteredServiceProvider<Permission> rsp2 = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp2 == null) {
            return false;
        }
        // 绑定
        GlobalData.VaultPerm = rsp2.getProvider();

        // 空值检查
        return GlobalData.VaultEco != null && GlobalData.VaultPerm != null;
    }

    private boolean bindWolfyUtilities() {
        // 创建API实例
        GlobalData.WolfyAPI = WolfyUtilities.getOrCreateAPI(this);
        // 设置前缀
        GlobalData.WolfyAPI.setCHAT_PREFIX(GlobalData.Config.getString("global-data.prefix"));
        GlobalData.WolfyAPI.setCONSOLE_PREFIX(GlobalData.Config.getString("NewNanCity"));
        // 多语言模块
        this.saveResource("lang/zh-CN.json", true);
        GlobalData.WolfyLanguageAPI = GlobalData.WolfyAPI.getLanguageAPI();
        try{
            GlobalData.WolfyLanguageAPI.registerLanguage(new Language(this, "zh-CN"));
        }
        catch (IOException e) {
            GlobalData.printERROR("无法打开语言文件！");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
        // 创建背包界面API实例
        GlobalData.WolfyInventoryAPI = GlobalData.WolfyAPI.getInventoryAPI();

        // demo
        GuiCluster cluster = GlobalData.WolfyInventoryAPI.getOrRegisterGuiCluster("none");
        GuiWindow window = new GuiWindow("main_menu", GlobalData.WolfyInventoryAPI, InventoryType.CHEST);
        ButtonState bstate = new ButtonState("settings", Material.OAK_LOG);
        DummyButton button = new DummyButton("settings", bstate);
        window.registerButton(button);
        cluster.registerGuiWindow(window);

        return true;
    }

    /**
     * 从插件文件夹下面读取对应的配置文件
     * @param file 文件名
     * @return 配置实例
     */
    public YamlConfiguration loadConf(String file) {
        File fp = new File(this.getDataFolder(), file);
        if (!fp.exists()) {
            this.saveResource(file, true);
        }
        return YamlConfiguration.loadConfiguration(fp);
    }

    /**
     * 保存配置到插件文件夹下的某个文件
     * @param file 文件名
     * @param conf 配置实例
     */
    public void saveConf(String file, YamlConfiguration conf) {
        try{
            File fp = new File(this.getDataFolder(), file);
            conf.save(fp);
        }
        catch (IOException e) {
            GlobalData.printERROR("无法保存配置文件: " + e.getMessage());
        }
    }

    /**
     * 设置世界的Gamerule
     */
    public void changeGamerules() {
        ConfigurationSection rules = GlobalData.Config.getConfigurationSection("module-world-setting");
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