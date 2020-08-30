package city.newnan.NewNanPlus.Fly;
import city.newnan.NewNanPlus.*;

import org.bukkit.Sound;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

public class FlyCommand {
    /**
     * 持久化访问全局数据
     */
    private final NewNanPlusGlobal GlobalData;

    public FlyCommand(NewNanPlusGlobal globalData) {
        GlobalData = globalData;
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
                GlobalData.sendMessage(sender, GlobalData.Config.getString("global-data.console-selfrun-refuse"));
                return false;
            }
            // 否则就是玩家执行
            Player _player = (Player) sender;
            // 如果他有自己的飞行权限
            if (!_player.hasPermission("newnanplus.fly.self")) {
                GlobalData.sendPlayerMessage(_player, GlobalData.Config.getString("global-data.no-permission-msg"));
                return false;
            }
            player = _player;
        } else {
            // 否则就是给别人开启/关闭
            // 检查权限
            if (!sender.hasPermission("newnanplus.fly.other")) {
                GlobalData.sendMessage(sender, GlobalData.Config.getString("global-data.no-permission-msg"));
                return false;
            }
            // 找到要操作的那个玩家
            player = GlobalData.Plugin.getServer().getPlayer(args[1]);
            if (player == null) {
                GlobalData.sendMessage(sender, GlobalData.Config.getString("global-data.player-offline-msg"));
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
            // 原本就能飞的不能进入飞行
            if (player.getAllowFlight()) {
                return true;
            }
            // 现金大于零才能飞
            if (GlobalData.VaultEco.getBalance(player) > 0.0) {
                // 添加玩家
                GlobalData.FlyingPlayers.put(player, new FlyingPlayer(System.currentTimeMillis(), player.getFlySpeed()));
                // 设置飞行和速度
                player.setFlySpeed((float)GlobalData.Config.getDouble("module-flyfee.fly-speed"));
                player.setAllowFlight(true);
                // 发送消息并播放声音
                GlobalData.sendPlayerMessage(player, GlobalData.Config.getString("module-flyfee.msg-begin"));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.0f);
            } else {
                // 不大于零就提示不能飞
                GlobalData.sendPlayerMessage(player, GlobalData.Config.getString("module-flyfee.msg-nofee"));
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
        if (!GlobalData.FlyingPlayers.containsKey(player)) {
            return false;
        }
        // 恢复玩家的状态
        player.setAllowFlight(false);
        player.setFlySpeed(GlobalData.FlyingPlayers.get(player).PreviousFlyingSpeed);
        if (sound) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.0f);
        }
        // 删除玩家
        GlobalData.FlyingPlayers.remove(player);
        // 发送飞行结束通知
        GlobalData.sendPlayerMessage(player, GlobalData.Config.getString("module-flyfee.msg-finish"));
        GlobalData.sendPlayerActionBar(player, GlobalData.Config.getString("module-flyfee.msg-finish"));
        return true;
    }

    public boolean listFlyingPlayers(CommandSender sender) {
        // 循环中不推荐使用 String 直接 +=，因为每次都会创建新的实例
        // 使用StringBuilder解决这个问题
        StringBuilder list = new StringBuilder();
        GlobalData.FlyingPlayers.forEach(((player, flyingPlayer) -> {
            list.append(player.getName()).append(" ");
        }));
        GlobalData.sendMessage(sender, "目前飞行人数：" + GlobalData.FlyingPlayers.size());
        if (GlobalData.FlyingPlayers.size() > 0) {
            GlobalData.sendMessage(sender, "飞行中：" + list);
        }
        return true;
    }
}

