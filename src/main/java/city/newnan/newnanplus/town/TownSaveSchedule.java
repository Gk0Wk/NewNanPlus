package city.newnan.newnanplus.town;

import city.newnan.newnanplus.NewNanPlusGlobal;
import org.bukkit.scheduler.BukkitRunnable;

public class TownSaveSchedule extends BukkitRunnable {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal globalData;

    public TownSaveSchedule(NewNanPlusGlobal globalData) {
        this.globalData = globalData;
    }

    @Override
    public void run() {
        globalData.townCommand.saveTowns();
    }
}
