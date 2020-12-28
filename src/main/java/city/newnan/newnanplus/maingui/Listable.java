package city.newnan.newnanplus.maingui;

import me.wolfyscript.utilities.api.inventory.GuiHandler;
import me.wolfyscript.utilities.api.inventory.button.Button;
import me.wolfyscript.utilities.api.inventory.cache.CustomCache;

import java.util.List;

public interface Listable {
    /**
     * 获取某个会话当前页项目(最多36个)
     * @param handler 会话对象
     * @return 本页面的按钮List
     */
    <T extends CustomCache> List<Button> getPage(GuiHandler<T> handler);

    /**
     * 获取某个会话当前页面的页码，从0开始
     * @param handler 会话对象
     * @return 页码，从0开始
     */
    <T extends CustomCache> int getPageIndex(GuiHandler<T> handler);

    /**
     * 获得某个会话该界面共有多少页
     * @param handler 会话对象
     * @return 总页数
     */
    <T extends CustomCache> int getPageCount(GuiHandler<T> handler);

    /**
     * 设置某个会话当前界面的页号
     * @param index 要设置的页号
     * @param handler 会话对象
     */
    <T extends CustomCache> void setPageIndex(int index, GuiHandler<T> handler);
}
