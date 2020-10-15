package city.newnan.newnanplus.feefly;

import city.newnan.newnanplus.NewNanPlusGlobal;
import city.newnan.newnanplus.NewNanPlusModule;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.MessageFormat;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class FeeFly extends BukkitRunnable implements Listener, NewNanPlusModule {
    /**
     * 持久化访问全局数据
     */
    private final NewNanPlusGlobal globalData;

    /** 正在飞行中的玩家，ConcurrentHashMap具有高并发性 */
    public final ConcurrentHashMap<Player, FlyingPlayer> flyingPlayers = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * @param globalData NewNanPlusGlobal实例，用于持久化存储和访问全局数据
     */
    public FeeFly(NewNanPlusGlobal globalData) {
        this.globalData = globalData;
        reloadConfig();
        runTaskTimer(this.globalData.plugin, 0, tickPerCount);
        this.globalData.plugin.getServer().getPluginManager().registerEvents(this, this.globalData.plugin);
    }

    private float flySpeed;
    private double costPerCount;
    private long tickPerCount;
    private double costPerSecond;
    private String flyStartMessage;
    private String flyEndMessage;
    private String actionbarBypassMessage;
    private String actionbarFeeMessage;
    private String lessFeeWarningMessage;
    private String noFeeMessage;

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        // 获取配置实例
        FileConfiguration config = globalData.configManager.get("config.yml");
        // 加载配置内容
        flySpeed = (float) config.getDouble("module-flyfee.fly-speed");
        costPerCount = config.getDouble("module-flyfee.cost-per-count");
        tickPerCount = config.getLong("module-flyfee.tick-per-count");
        costPerSecond = (20.0 / tickPerCount) * costPerCount;
        flyStartMessage = config.getString("module-flyfee.msg-begin");
        flyEndMessage = config.getString("module-flyfee.msg-finish");
        actionbarBypassMessage = config.getString("module-flyfee.msg-actionbar-bypass");
        actionbarFeeMessage = config.getString("module-flyfee.msg-actionbar");
        lessFeeWarningMessage = config.getString("module-flyfee.msg-feewraning");
        noFeeMessage = config.getString("module-flyfee.msg-nofee");
    }

    /**
     * 定时任务入口函数
     */
    @Override
    public void run() {
        if (flyingPlayers.size() > 0) {
            // 不能在遍历的时候删除元组，所以需要暂时记录
            Vector<Player> ToDeleteFlyingPlayer = new Vector<>();

            // 遍历飞行玩家 - 改用Lambda forEach
            flyingPlayers.forEach(((player, flyingPlayer) -> {
                if (player.hasPermission("newnanplus.fly.free")) {
                    globalData.sendPlayerActionBar(
                            player, MessageFormat.format(actionbarBypassMessage, player.getName()));
                    // lambda表达式中要用return跳过本次调用，相当于for的continue
                    return;
                }
                // 获取玩家现金金额
                double balance = globalData.vaultEco.getBalance(player);
                // 如果玩家还有现金
                if (balance > 0.0) {
                    int remain_second = (int)(balance / costPerSecond);
                    globalData.vaultEco.withdrawPlayer(player, Math.min(balance, costPerCount));
                    globalData.sendPlayerActionBar(player,
                            MessageFormat.format(actionbarFeeMessage, formatSecond(remain_second), balance));

                    // 如果只能飞一分钟以内，就警告
                    if (remain_second <= 60.0) {
                        String _msg = ChatColor.translateAlternateColorCodes('&', lessFeeWarningMessage);
                        player.sendTitle(_msg, null, 1, 7, 2);
                    }
                } else {
                    // 如果玩家没钱了，就踢出去
                    // 不能直接从里面删除，不太好，会让迭代器受损，所以先登记，for完了再删
                    ToDeleteFlyingPlayer.add(player);
                }
            }));

            // 删掉刚才需要踢除的
            ToDeleteFlyingPlayer.forEach((player) -> cancelFly(player, true));
            ToDeleteFlyingPlayer.clear();
        }
    }

    // 监听方法

    /**
     * 玩家退出事件监听函数
     * @param event 玩家退出事件实例
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 取消飞行
        cancelFly(event.getPlayer(), false);
    }

    /**
     * 玩家切换世界事件监听函数
     * @param event 玩家切换世界事件实例
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // 如果新的世界没有该权限，就取消玩家的飞行
        if (!event.getPlayer().hasPermission("newnanplus.fly.self"))
            cancelFly(event.getPlayer(), true);
    }

    /**
     * 玩家切换游戏模式事件监听函数
     * @param event 玩家切换游戏模式事件实例
     */
    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        // 如果玩家切换成创造或者旁观者模式，就取消玩家的飞行
        if (event.getNewGameMode().equals(GameMode.CREATIVE) || event.getNewGameMode().equals(GameMode.SPECTATOR))
            cancelFly(event.getPlayer(), true);
    }

    /**
     * 玩家死亡事件监听函数
     * @param event 玩家死亡事件实例
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 别飞了，给我下来
        cancelFly(event.getEntity(), false);
    }

    // 常规方法

    /**
     * 格式化时间
     * @param second 待格式化的秒数
     * @return 格式化的字符串
     */
    private static String formatSecond(int second) {
        if (second < 60) {
            return second + "秒";
        }else if (second < 3600) {
            int m = second / 60;
            int s = second % 60;
            return m + "分" + s + "秒";
        }else if (second < 86400) {
            int h = second / 3600;
            int m = (second % 3600) / 60;
            int s = (second % 3600) % 60;
            return h + "小时" + m + "分" + s + "秒";
        } else {
            int d = (second / 3600) / 24;
            int h = (second / 3600) % 24;
            int m = (second % 3600) / 60;
            int s = (second % 3600) % 60;
            return d + "天" + h + "小时" + m + "分" + s + "秒";
        }
    }

    /**
     * /nnp fly 启动付费飞行模式
     * @param sender 发送者实例
     * @param args 参数
     * @return 正确使用了命令，返回true，反之
     */
    public boolean applyFly(CommandSender sender, String[] args) {
        // 开启/关闭飞行的对象
        Player player;
        // 只有一个参数，说明是给自己开启/关闭
        if (args.length == 1) {
            // 控制台不能给自己开启/关闭
            if (sender instanceof ConsoleCommandSender) {
                globalData.sendMessage(sender, globalData.globalMessage.get("REFUSE_CONSOLE_SELFRUN"));
                return false;
            }
            // 否则就是玩家执行
            Player _player = (Player) sender;
            // 如果他有自己的飞行权限
            if (!_player.hasPermission("newnanplus.fly.self")) {
                globalData.sendPlayerMessage(_player, globalData.globalMessage.get("NO_PERMISSION"));
                return false;
            }
            player = _player;
        } else {
            // 否则就是给别人开启/关闭
            // 检查权限
            if (!sender.hasPermission("newnanplus.fly.other")) {
                globalData.sendMessage(sender, globalData.globalMessage.get("NO_PERMISSION"));
                return false;
            }
            // 找到要操作的那个玩家
            player = globalData.plugin.getServer().getPlayer(args[1]);
            if (player == null) {
                globalData.sendMessage(sender, globalData.globalMessage.get("PLAYER_OFFLINE"));
                return true;
            }
        }
        // 开启/关闭
        return makeFly(player);
    }

    /**
     * 让一个玩家进入/退出付费飞行模式
     * @param player 玩家实例
     * @return 成功开启/关闭返回true，反之
     */
    private boolean makeFly(Player player) {
        // 检查这个玩家是否在飞行
        if (!cancelFly(player, true)) {
            // 不在飞行，就开启飞行
            // 原本在创造或者观察者模式不能进入付费飞行
            if (player.getGameMode().equals(GameMode.CREATIVE) || player.getGameMode().equals(GameMode.SPECTATOR)) {
                return true;
            }
            // 原本就能飞的不能进入付费飞行
            if (player.getAllowFlight()) {
                return true;
            }
            // 现金大于零才能飞
            if (globalData.vaultEco.getBalance(player) > 0.0) {
                // 添加玩家
                flyingPlayers.put(player, new FlyingPlayer(System.currentTimeMillis(), player.getFlySpeed()));
                // 如果玩家在疾跑，应当取消它，否则飞起来之后会快
                player.setSprinting(false);
                // 设置飞行和速度
                player.setFlySpeed(flySpeed);
                player.setAllowFlight(true);
                // 发送消息并播放声音
                globalData.sendPlayerMessage(player, flyStartMessage);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.0f);
            } else {
                // 不大于零就提示不能飞
                globalData.sendPlayerMessage(player, noFeeMessage);
            }
        }
        return true;
    }

    /**
     * 去掉某个玩家的飞行
     * @param player 要取消的玩家
     * @param sound 是否同时播放声音
     * @return 如果玩家之前在飞行名单里，就返回true，反之
     */
    public boolean cancelFly(Player player, boolean sound) {
        // 不存在于列表就不取消
        if (!flyingPlayers.containsKey(player)) {
            return false;
        }
        // 恢复玩家的状态
        player.setAllowFlight(false);
        player.setFlySpeed(flyingPlayers.get(player).previousFlyingSpeed);
        if (sound) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.0f);
        }
        // 删除玩家
        flyingPlayers.remove(player);
        // 发送飞行结束通知
        globalData.sendPlayerMessage(player, flyEndMessage);
        globalData.sendPlayerActionBar(player, flyEndMessage);
        return true;
    }

    public boolean listFlyingPlayers(CommandSender sender) {
        // 循环中不推荐使用 String 直接 +=，因为每次都会创建新的实例
        // 使用StringBuilder解决这个问题
        StringBuilder list = new StringBuilder();
        flyingPlayers.forEach(((player, flyingPlayer) -> list.append(player.getName()).append(" ")));
        globalData.sendMessage(sender, "目前飞行人数：" + flyingPlayers.size());
        if (flyingPlayers.size() > 0) {
            globalData.sendMessage(sender, "飞行中：" + list);
        }
        return true;
    }
}