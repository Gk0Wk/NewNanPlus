package city.newnan.newnanplus.settingsgui;

import city.newnan.newnanplus.maingui.GuiCache;
import city.newnan.newnanplus.maingui.GuiUtils;
import me.wolfyscript.utilities.api.inventory.gui.GuiCluster;
import me.wolfyscript.utilities.api.inventory.gui.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.gui.GuiWindow;
import me.wolfyscript.utilities.api.inventory.gui.button.buttons.ActionButton;
import org.bukkit.Material;

public class SettingsMenu extends GuiWindow<GuiCache> {
    public SettingsMenu(GuiCluster<GuiCache> cluster) {
        super(cluster, "settings", 54);
    }

    public void onInit() {
        // 设置
        registerButton(new ActionButton<>("tpa_blocklist", Material.BARRIER,
                (cache, guiHandler, player, inventory, slot, event) -> {
                    guiHandler.openWindow("tpa_blocklist");
                    return true;
                }));
    }

    @Override
    public void onUpdateSync(GuiUpdate<GuiCache> guiUpdate) {

    }

    public void onUpdateAsync(GuiUpdate<GuiCache> update) {
        // 顶栏
        GuiUtils.setTopBar(update);
        update.setButton(9, "tpa_blocklist");
    }
}
