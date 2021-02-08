package city.newnan.newnanplus.maingui;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.settingsgui.SettingsMenu;
import city.newnan.newnanplus.teleport.TPABlockList;
import me.wolfyscript.utilities.api.inventory.gui.GuiCluster;
import me.wolfyscript.utilities.api.inventory.gui.button.Button;

public class NoneCluster extends GuiCluster<GuiCache> {
    private NewNanPlus plugin;

    public NoneCluster(String clusterID, NewNanPlus plugin) {
        super(plugin.inventoryAPI, clusterID);
        this.plugin = plugin;
    }

    @Override
    public void onInit() {
        try {
            GuiUtils.init(plugin, this);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        registerGuiWindow(new MainMenu(this));
        registerGuiWindow(new Ranks(this));
        registerGuiWindow(new ToolKits(this));
        registerGuiWindow(new SettingsMenu(this));
        registerGuiWindow(new TPABlockList(this));
    }

    public void myRegisterButton(Button<GuiCache> button) {
        registerButton(button);
    }
}
