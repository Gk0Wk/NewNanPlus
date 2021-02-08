package city.newnan.newnanplus.maingui;

import me.wolfyscript.utilities.api.inventory.gui.GuiHandler;
import me.wolfyscript.utilities.api.inventory.gui.button.Button;

import java.util.List;

public interface Listable {
    /**
     * 获取某个会话当前页项目(最多36个)
     * @param handler 会话对象
     * @return 本页面的按钮List
     */
    List<Button<GuiCache>> getPage(GuiHandler<GuiCache> handler);

    /**
     * 获取某个会话当前页面的页码，从0开始
     * @param handler 会话对象
     * @return 页码，从0开始
     */
    int getPageIndex(GuiHandler<GuiCache> handler);

    /**
     * 获得某个会话该界面共有多少页
     * @param handler 会话对象
     * @return 总页数
     */
    int getPageCount(GuiHandler<GuiCache> handler);

    /**
     * 设置某个会话当前界面的页号
     * @param index 要设置的页号
     * @param handler 会话对象
     */
    void setPageIndex(int index, GuiHandler<GuiCache> handler);
}
