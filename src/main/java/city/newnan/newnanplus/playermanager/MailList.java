package city.newnan.newnanplus.playermanager;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.maingui.GuiCache;
import city.newnan.newnanplus.maingui.GuiUtils;
import city.newnan.newnanplus.maingui.Listable;
import city.newnan.newnanplus.utility.PlayerConfig;
import me.wolfyscript.utilities.api.inventory.gui.GuiCluster;
import me.wolfyscript.utilities.api.inventory.gui.GuiHandler;
import me.wolfyscript.utilities.api.inventory.gui.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.gui.GuiWindow;
import me.wolfyscript.utilities.api.inventory.gui.button.Button;
import me.wolfyscript.utilities.api.inventory.gui.button.ButtonState;
import me.wolfyscript.utilities.api.inventory.gui.button.buttons.ActionButton;
import me.wolfyscript.utilities.api.inventory.gui.button.buttons.DummyButton;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MailList extends GuiWindow<GuiCache> implements Listable {
    public MailList(GuiCluster<GuiCache> cluster) { super(cluster, "mail_list", 54); }

    public void onInit() {
        loreString = NewNanPlus.getPlugin().wolfyLanguageAPI.replaceKey(
                "$inventories.none.mail_list.items.mail.lore$").toArray(new String[0]);
        readTitleString = NewNanPlus.getPlugin().wolfyLanguageAPI.replaceColoredKeys(
                "$inventories.none.mail_list.items.mail.read$");
        unreadTitleString = NewNanPlus.getPlugin().wolfyLanguageAPI.replaceColoredKeys(
                "$inventories.none.mail_list.items.mail.unread$");
        deletedTitleString = NewNanPlus.getPlugin().wolfyLanguageAPI.replaceColoredKeys(
                "$inventories.none.mail_list.items.mail.deleted$");
    }

    @Override
    public void onUpdateSync(GuiUpdate<GuiCache> guiUpdate) {
        // 顶栏
        GuiUtils.setTopBar(guiUpdate);
        GuiUtils.setListPage(guiUpdate, this);
    }

    @Override
    public void onUpdateAsync(GuiUpdate<GuiCache> guiUpdate) {

    }

    private String[] loreString;
    private String readTitleString, unreadTitleString, deletedTitleString;

    /**
     * 获取某个会话当前页项目(最多36个)
     *
     * @param handler 会话对象
     * @return 本页面的按钮List
     */
    @Override
    public List<Button<GuiCache>> getPage(GuiHandler<GuiCache> handler) {
        int index = getPageIndex(handler);
        try {
            List<String> mails = new ArrayList<>();
            List<String> unreadMailList = PlayerConfig.getPlayerConfig(Objects.requireNonNull(handler.getPlayer())).getUnreadEmails();
            List<String> readMailList = PlayerConfig.getPlayerConfig(Objects.requireNonNull(handler.getPlayer())).getReadEmails();
            mails.addAll(unreadMailList);
            mails.addAll(readMailList);
            List<String> subMailList = mails.subList(index * 36, ((getPageCount(handler) - index) <= 1) ?
                    mails.size() : (36 * (index + 1)));
            List<Button<GuiCache>> mailButtons = new ArrayList<>();

            MailSystem instance = (city.newnan.newnanplus.playermanager.MailSystem) NewNanPlus.getPlugin()
                    .getModule(city.newnan.newnanplus.playermanager.MailSystem.class);

            HashMap<String, Button<GuiCache>> buttons = GuiUtils.getWindowButtons(this);
            assert buttons != null;
            subMailList.forEach(mailName -> {
                Mail mail = instance.getMail(mailName);
                if (mail == null) {
                    if (buttons.containsKey(mailName)) {
                        mailButtons.add(buttons.get(mailName));
                    } else {
                        ButtonState<GuiCache> state = new ButtonState<>(Mail.defaultIcon,
                                mailName + deletedTitleString, null, null);
                        DummyButton<GuiCache> button = new DummyButton<>(mailName, state);
                        buttons.put(mailName, button);
                        mailButtons.add(button);
                    }
                } else {
                    boolean ifRead = readMailList.contains(mailName);
                    String oriMailName = mailName;
                    mailName = mailName + (ifRead ? "#Read" : "#Unread");
                    if (buttons.containsKey(mailName)) {
                        mailButtons.add(buttons.get(mailName));
                    } else {
                        ButtonState<GuiCache> state = new ButtonState<>(mail.icon,
                                "§r" + mail.title + (ifRead ? readTitleString : unreadTitleString),
                                loreString, (cache, guiHandler, player, inventory, slot, event) -> {
                                    if (event instanceof InventoryClickEvent) {
                                        if (((InventoryClickEvent)event).getClick().isLeftClick()) {
                                            try {
                                                boolean resultRead = instance.showEmail(player, mail, ifRead);
                                                if (ifRead ^ resultRead) {
                                                    unreadMailList.remove(oriMailName);
                                                    readMailList.add(oriMailName);
                                                    PlayerConfig.getPlayerConfig(Objects.requireNonNull(handler.getPlayer())).commit();
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    return true;
                                });
                        ActionButton<GuiCache> button = new ActionButton<>(mailName, state);
                        buttons.put(mailName, button);
                        mailButtons.add(button);
                    }
                }
            });
            return mailButtons;
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
    public int getPageIndex(GuiHandler<GuiCache> handler) {
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
    public int getPageCount(GuiHandler<GuiCache> handler) {
        try {
            PlayerConfig config = PlayerConfig.getPlayerConfig(Objects.requireNonNull(handler.getPlayer()));
            return (config.getReadEmails().size() + config.getUnreadEmails().size() + 35) / 36;
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
    public void setPageIndex(int index, GuiHandler<GuiCache> handler) {
        handler.getCustomCache().getWindowCache(this).
                put(Objects.requireNonNull(handler.getPlayer()).getUniqueId().toString(), index);
    }
}
