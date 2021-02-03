package city.newnan.newnanplus.teleport;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.maingui.GuiCache;
import city.newnan.newnanplus.maingui.GuiUtils;
import city.newnan.newnanplus.maingui.Listable;
import city.newnan.newnanplus.powertools.SkullKits;
import city.newnan.newnanplus.utility.PlayerConfig;
import me.wolfyscript.utilities.api.inventory.GuiHandler;
import me.wolfyscript.utilities.api.inventory.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.GuiWindow;
import me.wolfyscript.utilities.api.inventory.InventoryAPI;
import me.wolfyscript.utilities.api.inventory.button.Button;
import me.wolfyscript.utilities.api.inventory.button.ButtonState;
import me.wolfyscript.utilities.api.inventory.button.buttons.ActionButton;
import me.wolfyscript.utilities.api.inventory.cache.CustomCache;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.util.*;

public class TPABlockList extends GuiWindow implements Listable {
    public TPABlockList(InventoryAPI<GuiCache> inventoryAPI) { super("tpa_blocklist", inventoryAPI, 54); }

    public void onInit() {
        loreString = NewNanPlus.getPlugin().wolfyLanguageAPI.replaceKey(
                "$inventories.none.tpa_blocklist.items.head.lore$").toArray(new String[0]);
        helpString = NewNanPlus.getPlugin().wolfyLanguageAPI.replaceKey(
                "$inventories.none.tpa_blocklist.items.head.help$").toArray(new String[0]);
    }

    public void onUpdateAsync(GuiUpdate update) {
        // 顶栏
        GuiUtils.setTopBar(update);
        GuiUtils.setListPage(update, this);
    }

    private String[] loreString, helpString;
    /**
     * 获取某个会话当前页项目(最多36个)
     *
     * @param handler 会话对象
     * @return 本页面的按钮List
     */
    @Override
    public <T extends CustomCache> List<Button> getPage(GuiHandler<T> handler) {
        int index = getPageIndex(handler);
        try {
            List<String> blockUUIDList =
                    PlayerConfig.getPlayerConfig(Objects.requireNonNull(handler.getPlayer())).getTpaBlockList();
            List<String> subBlockList = blockUUIDList.subList(index * 36, ((getPageCount(handler) - index) <= 1) ?
                    blockUUIDList.size() : (36 * (index + 1)));
            List<Button> playerButtons = new ArrayList<>();
            Server server =  NewNanPlus.getPlugin().getServer();

            Tpa instance = (city.newnan.newnanplus.teleport.Tpa) NewNanPlus.getPlugin().getModule(
                    city.newnan.newnanplus.teleport.Tpa.class);

            HashMap<String, Button> buttons = GuiUtils.getWindowButtons(this);
            assert buttons != null;
            subBlockList.forEach(uuidString -> {
                if (buttons.containsKey(uuidString)) {
                    playerButtons.add(buttons.get(uuidString));
                } else {
                    OfflinePlayer _player = server.getOfflinePlayer(UUID.fromString(uuidString));
                    ButtonState state = new ButtonState(SkullKits.getSkull(_player), "§r§l" + _player.getName(),
                            helpString, loreString,
                            (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                                if (inventoryClickEvent.isRightClick()) {
                                    try {
                                        instance.removeFromBlackList(player, _player);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                return true;
                            });
                    ActionButton blockedPlayer = new ActionButton(uuidString, state);
                    buttons.put(uuidString, blockedPlayer);
                    playerButtons.add(blockedPlayer);
                }
            });
            return playerButtons;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 获取某个会话当前页面的页码，从0开始
     *
     * @param handler 会话对象
     * @return 页码，从0开始
     */
    @Override
    public <T extends CustomCache> int getPageIndex(GuiHandler<T> handler) {
        Integer index = (Integer) handler.getCustomCache().getWindowCache(this).
                get(Objects.requireNonNull(handler.getPlayer()).getUniqueId().toString());
        if (index == null || index < 0) {
            setPageIndex(index = 0, handler);
        } else if (index >= getPageCount(handler)) {
            index = getPageCount(handler) - 1;
            if (index < 0)
                index = 0;
            setPageIndex(index, handler);
        }
        return index;
    }

    /**
     * 获得某个会话该界面共有多少页
     *
     * @param handler 会话对象
     * @return 总页数
     */
    @Override
    public <T extends CustomCache> int getPageCount(GuiHandler<T> handler) {
        try {
            return (PlayerConfig.getPlayerConfig(Objects.requireNonNull(handler.getPlayer())).
                    getTpaBlockList().size() + 35) / 36;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 设置某个会话当前界面的页号
     *
     * @param index   要设置的页号
     * @param handler 会话对象
     */
    @Override
    public <T extends CustomCache> void setPageIndex(int index, GuiHandler<T> handler) {
        handler.getCustomCache().getWindowCache(this).
                put(Objects.requireNonNull(handler.getPlayer()).getUniqueId().toString(), index);
    }
}
