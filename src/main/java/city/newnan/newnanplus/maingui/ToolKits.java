package city.newnan.newnanplus.maingui;

import me.wolfyscript.utilities.api.inventory.GuiHandler;
import me.wolfyscript.utilities.api.inventory.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.GuiWindow;
import me.wolfyscript.utilities.api.inventory.InventoryAPI;
import me.wolfyscript.utilities.api.inventory.button.Button;
import me.wolfyscript.utilities.api.inventory.button.buttons.ActionButton;
import me.wolfyscript.utilities.api.inventory.cache.CustomCache;
import me.wolfyscript.utilities.api.utils.inventory.PlayerHeadUtils;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ToolKits extends GuiWindow implements Listable  {
    public ToolKits(InventoryAPI<?> inventoryAPI) { super("toolkits", inventoryAPI, 54); }

    private final ArrayList<Button> tools = new ArrayList<>();
    private int pageCount;

    public void onInit() {
        // 家
        tools.add(new ActionButton("home", Material.RED_BED,
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    player.performCommand("home");
                    return true;
                }));
        // 主城
        tools.add(new ActionButton("city",
                PlayerHeadUtils.getViaURL("4528ed45802400f465b5c4e3a6b7a9f2b6a5b3d478b6fd84925cc5d988391c7d"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    player.performCommand("city");
                    return true;
                }));
        // 创造区
        tools.add(new ActionButton("ctp", Material.WOODEN_AXE,
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    player.performCommand("nnp ctp");
                    return true;
                }));
        // 资源区
        tools.add(new ActionButton("resource",
                PlayerHeadUtils.getViaURL("8112f87cee578894e2d07253abb1466247cee48f1727bb9d1eac53f8e0571012"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    player.performCommand("resource tp");
                    return true;
                }));

        tools.forEach(this::registerButton);
        pageCount = (tools.size() + 35) / 36;
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
        return tools.subList(index * 36, ((pageCount - index) <= 1) ? tools.size() : (36 * (index + 1)));
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
