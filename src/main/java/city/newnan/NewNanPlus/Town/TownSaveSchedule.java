package city.newnan.NewNanPlus.Town;

import city.newnan.NewNanPlus.NewNanPlusGlobal;
import org.bukkit.scheduler.BukkitRunnable;

public class TownSaveSchedule extends BukkitRunnable {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal GlobalData;

    public TownSaveSchedule(NewNanPlusGlobal globalData) {
        this.GlobalData = globalData;
    }

    @Override
    public void run() {
        GlobalData.TownCommand.saveTowns();
    }
}
