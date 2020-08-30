package city.newnan.NewNanPlus.LaggAnalyzer;
import city.newnan.NewNanPlus.NewNanPlusGlobal;

import org.bukkit.Chunk;
import org.bukkit.block.Hopper;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;

import java.io.FileWriter;
import java.io.IOException;


public class LACommand {
    /**
     * 持久化访问全局数据
     */
    NewNanPlusGlobal GlobalData;
    public LACommand(NewNanPlusGlobal globalData) {
        GlobalData = globalData;
    }

    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        if (e.getInitiator().getType() == InventoryType.HOPPER) {
            Hopper hopper = (Hopper) e.getInitiator().getHolder();
            Chunk chunk = hopper.getChunk();
            String key = chunk.getX()+","+chunk.getZ();
            Integer x = GlobalData.HopperMap.get(key);
            if (x == null) {
                x = 1;
            } else {
                x++;
            }
            GlobalData.HopperMap.put(key, x);
        }
    }

    public boolean genHopperReport() {
        try {
            FileWriter fp = new FileWriter("hopper.csv");
            fp.write("Event_Count,Chunk_X,Chunk_Z\n");
            GlobalData.HopperMap.forEach((key, value) -> {
                try{
                    fp.write(value+","+key+"\n");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            });
            fp.close();
            GlobalData.printINFO("漏斗报告已保存至 hopper.csv");
        } catch (IOException e) {
            GlobalData.printERROR("无法保存报告: " + e.getMessage());
        }
        return true;
    }
}
