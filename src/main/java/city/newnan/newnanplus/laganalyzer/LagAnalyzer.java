package city.newnan.newnanplus.laganalyzer;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions.ModuleOffException;
import org.bukkit.Chunk;
import org.bukkit.block.Hopper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;


public class LagAnalyzer implements NewNanPlusModule {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    private final HashMap<String, Integer> hopperMap = new HashMap<>();

    /**
     * 构造函数
     */
    public LagAnalyzer() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManagers.get("config.yml").getNode("module-lagganalyzer", "enable").getBoolean(false)) {
            throw new ModuleOffException();
        }
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {

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
    public void executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {

    }

    /**
     * 监控漏斗的激活情况
     * @param event 物品栏中物品移动的事件
     */
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getInitiator().getType() == InventoryType.HOPPER) {
            Hopper hopper = (Hopper) event.getInitiator().getHolder();
            assert hopper != null;
            Chunk chunk = hopper.getChunk();
            String key = chunk.getX()+","+chunk.getZ();
            Integer x = hopperMap.get(key);
            if (x == null) {
                x = 1;
            } else {
                x++;
            }
            hopperMap.put(key, x);
        }
    }

    /**
     * 获得漏斗活动计数的区块分布
     * @return 成功导出则返回true，反之
     */
    public boolean genHopperReport() {
        try {
            FileWriter fp = new FileWriter("hopper.csv");
            fp.write("Event_Count,Chunk_X,Chunk_Z\n");
            hopperMap.forEach((key, value) -> {
                try{
                    fp.write(value+","+key+"\n");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            });
            fp.close();
            plugin.messageManager.info("漏斗报告已保存至 hopper.csv");
        } catch (IOException e) {
            plugin.messageManager.info("无法保存报告: " + e.getMessage());
            return false;
        }
        return true;
    }
}
