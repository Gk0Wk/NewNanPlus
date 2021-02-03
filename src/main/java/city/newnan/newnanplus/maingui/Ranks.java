package city.newnan.newnanplus.maingui;

import me.wolfyscript.utilities.api.inventory.GuiHandler;
import me.wolfyscript.utilities.api.inventory.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.GuiWindow;
import me.wolfyscript.utilities.api.inventory.InventoryAPI;
import me.wolfyscript.utilities.api.inventory.button.Button;
import me.wolfyscript.utilities.api.inventory.button.buttons.ActionButton;
import me.wolfyscript.utilities.api.inventory.cache.CustomCache;
import me.wolfyscript.utilities.api.utils.inventory.PlayerHeadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Ranks extends GuiWindow implements Listable {
    public Ranks(InventoryAPI<?> inventoryAPI) { super("ranks", inventoryAPI, 54); }

    private final ArrayList<Button> rankList = new ArrayList<>();
    private int pageCount;

    public void onInit() {
        // 财富总榜
        rankList.add(new ActionButton("balance",
                PlayerHeadUtils.getViaURL("e36e94f6c34a35465fce4a90f2e25976389eb9709a12273574ff70fd4daa6852"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    player.performCommand("essentials:baltop");
                    return true;
                }));
        // 成就总榜
        rankList.add(new ActionButton("achievement_total",
                PlayerHeadUtils.getViaURL("1b8fe1c44acbeeb918d38bc42d550bedd5c3dd049889fd9eeea1160ab8b6a"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    player.performCommand("aach top");
                    return true;
                }));
        // 成就月榜
        rankList.add(new ActionButton("achievement_monthly",
                PlayerHeadUtils.getViaURL("4d9dedfe01d8efd96e7dca2e930d984568c411ba83449c190af0c5ef052f2729"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    player.performCommand("aach month");
                    return true;
                }));
        // 成就周榜
        rankList.add(new ActionButton("achievement_weekly",
                PlayerHeadUtils.getViaURL("3d80617f6b451d0c2fbc1bb939cfeffa06fa375dc57d5e8f61b8e3fa40452b4b"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    player.performCommand("aach week");
                    return true;
                }));

        rankList.forEach(this::registerButton);
        pageCount = (rankList.size() + 35) / 36;
    }
    public void onUpdateAsync(GuiUpdate update) {
        GuiUtils.setTopBar(update);
        GuiUtils.setListPage(update, this);
    }

    /**
     * 获取某个会话当前页项目(最多36个)
     *
     * @param handler 会话对象
     * @return 本页面的按钮List
     */
    @Override
    public <T extends CustomCache> List<Button> getPage(GuiHandler<T> handler) {
        int index = getPageIndex(handler);
        return rankList.subList(index * 36, ((pageCount - index) <= 1) ? rankList.size() : (36 * (index + 1)));
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
        } else if (index > pageCount) {
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
        return pageCount;
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
