package city.newnan.newnanplus.settingsgui;

import city.newnan.newnanplus.maingui.GuiCache;
import city.newnan.newnanplus.maingui.GuiUtils;
import me.wolfyscript.utilities.api.inventory.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.GuiWindow;
import me.wolfyscript.utilities.api.inventory.InventoryAPI;
import me.wolfyscript.utilities.api.inventory.button.buttons.ActionButton;
import org.bukkit.Material;

public class SettingsMenu extends GuiWindow {
    public SettingsMenu(InventoryAPI<GuiCache> inventoryAPI) {
        super("settings", inventoryAPI, 54);
    }

    public void onInit() {
        // 设置
        registerButton(new ActionButton("tpa_blocklist", Material.BARRIER,
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    guiHandler.changeToInv("none", "tpa_blocklist");
                    return true;
                }));
    }

    public void onUpdateAsync(GuiUpdate update) {
        // 顶栏
        GuiUtils.setTopBar(update);
        update.setButton(9, "tpa_blocklist");
    }
}
