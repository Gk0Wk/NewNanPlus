package city.newnan.newnanplus.cron;

import city.newnan.newnanplus.NewNanPlusGlobal;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * NewNanPlus 计划任务
 */
public class Cron extends BukkitRunnable implements Listener, NewNanPlusModule {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal globalData;

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public Cron(NewNanPlusGlobal globalData) throws Exception {
        this.globalData = globalData;
        // 是否禁用
        if (!globalData.configManager.get("cron.yml").getBoolean("enable", false)) {
            throw new ModuleExeptions.ModuleOffException();
        }
        reloadConfig();
        // 启动定时任务
        runTaskTimer(globalData.plugin, 0, 20);
        // 注册监听函数
        globalData.plugin.getServer().getPluginManager().registerEvents(this, globalData.plugin);
        globalData.cron = this;
    }

    /**
     * 重载模块配置
     */
    @Override
    public void reloadConfig() {
        FileConfiguration config = globalData.configManager.reload("cron.yml");
        this.tasks.clear();
        this.cacheInTimeTasks.clear();
        this.inTimeTasks.clear();
        this.outdatedTasks.clear();
        ConfigurationSection map = config.getConfigurationSection("schedule-tasks");
        assert map != null;
        for (String name : map.getKeys(false)) {
            List<String> commands = map.getStringList(name);
            addTask(name, commands.toArray(new String[0]));
        }
    }

    /**
     * 执行某个命令
     *
     * @param sender  发送指令者的实例
     * @param command 被执行的指令实例
     * @param token   指令的标识字符串
     * @param args    指令的参数
     */
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {

    }

    /**
     * 执行在服务器就绪时执行的命令，即on-server-ready
     * @param event 事件实例
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerLoad(ServerLoadEvent event) {
        List<String> commands = globalData.configManager.get("cron.yml").getStringList("on-server-ready");
        CommandSender sender = globalData.plugin.getServer().getConsoleSender();
        commands.forEach(command -> {
            globalData.printINFO("§a§lRun Command: §r" + command);
            globalData.plugin.getServer().dispatchCommand(sender, command);
        });
    }

    /**
     * 执行在插件加载完毕时执行的命令，即on-plugin-ready
     * 和on-server-ready不同，后者只会在服务器开启时执行
     */
    public void onPluginReady() {
        List<String> commands = globalData.configManager.get("cron.yml").getStringList("on-plugin-ready");
        CommandSender sender = globalData.plugin.getServer().getConsoleSender();
        commands.forEach(command -> {
            globalData.printINFO("§a§lRun Command: §r" + command);
            globalData.plugin.getServer().dispatchCommand(sender, command);
        });
    }

    /**
     * 执行在插件禁用时执行的命令，即on-plugin-disable
     */
    public void onPluginDisable() {
        List<String> commands = globalData.configManager.get("cron.yml").getStringList("on-plugin-disable");
        CommandSender sender = globalData.plugin.getServer().getConsoleSender();
        commands.forEach(command -> {
            globalData.printINFO("§a§lRun Command: §r" + command);
            globalData.plugin.getServer().dispatchCommand(sender, command);
        });
    }

    ArrayList<CronCommand> tasks = new ArrayList<>();
    // 1秒内即将执行的任务
    private final List<CronCommand> inTimeTasks = new ArrayList<>();
    private final List<CronCommand> outdatedTasks = new ArrayList<>();
    // 为防止卡顿导致错过一些任务(没有执行，但是错过了判断，被误认为是已过期任务)
    // 有一个缓冲池，在1s~60s后即将执行的命令也会在这里，这样如果这些任务过期了会立即执行
    // p.s. 不会有人写每秒都会运行的程序吧...
    private final List<CronCommand> cacheInTimeTasks = new ArrayList<>();

    /**
     * 在任务池中添加一个任务
     * @param cronExpression cron表达式
     * @param commands 任务要执行的指令
     */
    public void addTask(String cronExpression, String[] commands) {
        try {
            CronCommand task = new CronCommand(cronExpression, commands);
            this.tasks.add(task);
        }
        catch (Exception e) {
            globalData.printWARN(MessageFormat.format(globalData.wolfyLanguageAPI.
                    replaceColoredKeys("$module_message.cron.invalid_expression$"), cronExpression));
        }
    }

    /**
     * 定时检查模块的计时器
     */
    private int secondsCounter = 1;
    /**
     * 计时器的动态上限
     */
    private int counterBorder = 1;

    /**
     * 定时任务检查，每秒运行
     */
    @Override
    public void run() {
        // 小于计数上限，继续休眠
        if (secondsCounter < counterBorder) {
            secondsCounter++;
            return;
        }

        // 下一次任务的间隔毫秒数(不含失效任务)
        long nextMillisecond = Long.MAX_VALUE;

        // 当前时间戳
        long curMillisecond = System.currentTimeMillis();

        // 遍历所有任务
        for (CronCommand task : this.tasks) {
            long tmp = task.expression.getNextTime();

            // 忽略失效的，并清理之
            if (tmp == 0) {
                this.outdatedTasks.add(task);
                // 如果失效任务在缓冲列表中，肯定是漏掉了，赶快执行
                if (this.cacheInTimeTasks.contains(task)) {
                    this.cacheInTimeTasks.remove(task);
                    this.inTimeTasks.add(task);
                }
                continue;
            }

            tmp = tmp - curMillisecond;

            // 统计下一次任务的间隔时长(不含失效任务)
            if (tmp < nextMillisecond) {
                nextMillisecond = tmp;
            }

            // 一秒内即将执行的任务进入执行队列
            if (tmp <= 1000) {
                this.inTimeTasks.add(task);
                // 并从缓冲列表移除
                this.cacheInTimeTasks.remove(task);
                continue;
            }

            // 1s~60s进入缓冲列表
            if (tmp <= 60000) {
                if (!this.cacheInTimeTasks.contains(task)) {
                    this.cacheInTimeTasks.add(task);
                }
                continue;
            }

            // 到这里的肯定是60秒开外的任务
            // 如果这些任务中有些任务出现在缓冲区中
            // 说明漏掉了，尽快执行
            if (this.cacheInTimeTasks.contains(task)) {
                this.cacheInTimeTasks.remove(task);
                this.inTimeTasks.add(task);
            }
        }

        // 清理失效任务
        if (!this.outdatedTasks.isEmpty()) {
            this.outdatedTasks.forEach(task -> this.tasks.remove(task));
            this.outdatedTasks.clear();
        }

        // 执行一秒内到来的任务
        if (!this.inTimeTasks.isEmpty()) {
            this.globalData.plugin.getServer().getScheduler().runTaskLater(
                    globalData.plugin, this::runInSecond, 20);
        }

        // 计算下次检查时间
        counterBorder = getIntervalSeconds(nextMillisecond);
        secondsCounter = 1;
    }

    /**
     * 一段时间所对应的TICK数
     */
    private static final int TICKS_OF_12HOUR = 864000;
    private static final int TICKS_OF_6HOUR  = 432000;
    private static final int TICKS_OF_3HOUR  = 216000;
    private static final int TICKS_OF_1HOUR  = 72000;
    private static final int TICKS_OF_30MIN  = 36000;
    private static final int TICKS_OF_15MIN  = 18000;
    private static final int TICKS_OF_8MIN   = 9600;
    private static final int TICKS_OF_4MIN   = 4800;
    private static final int TICKS_OF_MINUTE = 1200;
    private static final int TICKS_OF_15SEC  = 300;
    private static final int TICKS_OF_5SEC   = 100;
    private static final int TICKS_OF_SECOND = 20;

    /**
     * 一段时间所对应的秒数
     */
    private static final int SECONDS_OF_12HOUR = 43200;
    private static final int SECONDS_OF_6HOUR  = 21600;
    private static final int SECONDS_OF_3HOUR  = 10800;
    private static final int SECONDS_OF_1HOUR  = 3600;
    private static final int SECONDS_OF_30MIN  = 1800;
    private static final int SECONDS_OF_15MIN  = 900;
    private static final int SECONDS_OF_8MIN   = 480;
    private static final int SECONDS_OF_4MIN   = 240;
    private static final int SECONDS_OF_MINUTE = 60;
    private static final int SECONDS_OF_15SEC  = 15;
    private static final int SECONDS_OF_5SEC   = 5;
    private static final int SECONDS_OF_SECOND = 1;

    /**
     * 一段时间所对应的毫秒数
     */
    private static final long MILLISECOND_OF_2DAY   = 172800000;
    private static final long MILLISECOND_OF_1DAY   = 86400000;
    private static final long MILLISECOND_OF_12HOUR = 43200000;
    private static final long MILLISECOND_OF_4HOUR  = 14400000;
    private static final long MILLISECOND_OF_2HOUR  = 7200000;
    private static final long MILLISECOND_OF_HOUR   = 3600000;
    private static final long MILLISECOND_OF_30MIN  = 1800000;
    private static final long MILLISECOND_OF_15MIN  = 900000;
    private static final long MILLISECOND_OF_5MIN   = 300000;
    private static final long MILLISECOND_OF_MINUTE = 60000;
    private static final long MILLISECOND_OF_30SEC  = 30000;

    /**
     * 根据毫秒差获得合适的休眠间隔(Tick为单位)
     * @param delta 毫秒差
     * @return 休眠间隔(Tick为单位)
     */
    @Deprecated
    private int getIntervalTicks(long delta) {
        // 小于30秒   - 每秒
        if (delta < MILLISECOND_OF_30SEC)
            return TICKS_OF_SECOND;

        // 小于1分钟  - 每5秒
        if (delta < MILLISECOND_OF_MINUTE)
            return TICKS_OF_5SEC;

        // 小于5分钟  - 每15秒
        if (delta < MILLISECOND_OF_5MIN)
            return TICKS_OF_15SEC;

        // 小于15分钟 - 每1分钟
        if (delta < MILLISECOND_OF_15MIN)
            return TICKS_OF_MINUTE;

        // 小于30分钟 - 每4分钟
        if (delta < MILLISECOND_OF_30MIN)
            return TICKS_OF_4MIN;

        // 小于1小时 - 每8分钟
        if (delta < MILLISECOND_OF_HOUR)
            return TICKS_OF_8MIN;

        // 小于2小时 - 每15分钟
        if (delta < MILLISECOND_OF_2HOUR)
            return TICKS_OF_15MIN;

        // 小于4小时 - 每半小时
        if (delta < MILLISECOND_OF_4HOUR)
            return TICKS_OF_30MIN;

        // 小于12小时 - 每1小时
        if (delta < MILLISECOND_OF_12HOUR)
            return TICKS_OF_1HOUR;

        // 小于1天 - 每3小时
        if (delta < MILLISECOND_OF_1DAY)
            return TICKS_OF_3HOUR;

        // 小于2天 - 每6小时
        if (delta < MILLISECOND_OF_2DAY)
            return TICKS_OF_6HOUR;

        // 其他 - 每12小时
        return TICKS_OF_12HOUR;
    }

    /**
     * 根据毫秒差获得合适的休眠间隔(秒为单位)
     * @param delta 毫秒差
     * @return 休眠间隔(秒为单位)
     */
    private int getIntervalSeconds(long delta) {
        // 小于30秒   - 每秒
        if (delta < MILLISECOND_OF_30SEC)
            return SECONDS_OF_SECOND;

        // 小于1分钟  - 每5秒
        if (delta < MILLISECOND_OF_MINUTE)
            return SECONDS_OF_5SEC;

        // 小于5分钟  - 每15秒
        if (delta < MILLISECOND_OF_5MIN)
            return SECONDS_OF_15SEC;

        // 小于15分钟 - 每1分钟
        if (delta < MILLISECOND_OF_15MIN)
            return SECONDS_OF_MINUTE;

        // 小于30分钟 - 每4分钟
        if (delta < MILLISECOND_OF_30MIN)
            return SECONDS_OF_4MIN;

        // 小于1小时 - 每8分钟
        if (delta < MILLISECOND_OF_HOUR)
            return SECONDS_OF_8MIN;

        // 小于2小时 - 每15分钟
        if (delta < MILLISECOND_OF_2HOUR)
            return SECONDS_OF_15MIN;

        // 小于4小时 - 每半小时
        if (delta < MILLISECOND_OF_4HOUR)
            return SECONDS_OF_30MIN;

        // 小于12小时 - 每1小时
        if (delta < MILLISECOND_OF_12HOUR)
            return SECONDS_OF_1HOUR;

        // 小于1天 - 每3小时
        if (delta < MILLISECOND_OF_1DAY)
            return SECONDS_OF_3HOUR;

        // 小于2天 - 每6小时
        if (delta < MILLISECOND_OF_2DAY)
            return SECONDS_OF_6HOUR;

        // 其他 - 每12小时
        return SECONDS_OF_12HOUR;
    }

    /**
     * 执行inTimeTask中的那些一秒内即将执行的任务
     */
    public void runInSecond() {
        CommandSender sender =  globalData.plugin.getServer().getConsoleSender();
        this.inTimeTasks.forEach(task -> {
            for (String command : task.commands) {
                globalData.plugin.getServer().dispatchCommand(sender,command);
            }
        });
        this.inTimeTasks.clear();
    }
}

class CronCommand {
    public final CronExpression expression;
    public final String[] commands;

    public CronCommand(String expression, String[] commands) {
        this.expression = new CronExpression(expression);
        this.commands = commands;
    }
}