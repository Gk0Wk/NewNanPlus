package city.newnan.newnanplus.maingui;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.powertools.SkullKits;
import me.wolfyscript.utilities.api.inventory.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.GuiWindow;
import me.wolfyscript.utilities.api.inventory.InventoryAPI;
import me.wolfyscript.utilities.api.inventory.button.buttons.ActionButton;
import me.wolfyscript.utilities.api.utils.chat.ClickData;
import me.wolfyscript.utilities.api.utils.chat.ClickEvent;
import me.wolfyscript.utilities.api.utils.chat.HoverEvent;
import me.wolfyscript.utilities.api.utils.inventory.PlayerHeadUtils;
import org.bukkit.Material;

import java.util.Objects;

public class MainMenu extends GuiWindow {
    public MainMenu(InventoryAPI<GuiCache> inventoryAPI) {
        super("main", inventoryAPI, 54);
    }

    public void onInit() {
        // 设置
        registerButton(new ActionButton("settings",
                PlayerHeadUtils.getViaURL("5949a18cb52c293fe7de7ba1014671340ed7ff8e5d705b2d60bf84d53148e04"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {

                    return true;
                }));
        // 玩家手册
        registerButton(new ActionButton("guide", Material.KNOWLEDGE_BOOK,
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {

                    return true;
                }));
        // 邮箱
        registerButton(new ActionButton("mail",
                PlayerHeadUtils.getViaURL("eb2815b99c13bfc55b4c5c2959d157a6233ab06186459233bc1e4d4f78792c69"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {

                    return true;
                }));
        // 称号
        registerButton(new ActionButton("title", Material.NAME_TAG,
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    NewNanPlus.getPlugin().messageManager.sendMessage(Objects.requireNonNull(guiHandler.getPlayer()),
                            NewNanPlus.getPlugin().wolfyLanguageAPI.replaceColoredKeys(
                                    "$global_message.function_not_open$"));
                    return true;
                }));
        // 成就
        registerButton(new ActionButton("achievement",
                PlayerHeadUtils.getViaURL("1a696e152363ad477ed6cb5a518bfea84b036148fecac0581f32c5b5396958"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    Objects.requireNonNull(guiHandler.getPlayer()).performCommand("aach list");
                    return true;
                }));
        // 排行榜
        registerButton(new ActionButton("rank",
                PlayerHeadUtils.getViaURL("af3034d24a85da31d67932c33e5f1821e219d5dcd9c2ba4f2559df48deea"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    guiHandler.changeToInv("none", "ranks");
                    return true;
                }));
        // 任务
        registerButton(new ActionButton("task", Material.WRITABLE_BOOK,
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    Objects.requireNonNull(guiHandler.getPlayer()).performCommand("betonquest:betonquestbackpack");
                    return true;
                }));
        // 小镇
        registerButton(new ActionButton("town",
                PlayerHeadUtils.getViaURL("cf7cdeefc6d37fecab676c584bf620832aaac85375e9fcbff27372492d69f"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    NewNanPlus.getPlugin().messageManager.sendMessage(Objects.requireNonNull(guiHandler.getPlayer()),
                            NewNanPlus.getPlugin().wolfyLanguageAPI.replaceColoredKeys(
                                    "$global_message.function_not_open$"));
                    return true;
                }));
        // 工具箱
        registerButton(new ActionButton("toolkit",
                PlayerHeadUtils.getViaURL("8362438ff4ecf8f4a2caa1277561c9513c9a986dbe38a80b9bafcbfed8b2a9c8"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    guiHandler.changeToInv("none", "toolkits");
                    return true;
                }));
        // 举报
        registerButton(new ActionButton("report",
                PlayerHeadUtils.getViaURL("6448e275313532f54c4ba21894809a23dce52af01ddd1e89fc7689481fab737e"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    getAPI().openBook(player, "NewNanCity", "举报", false,
                        new ClickData[][] {
                            {
                                new ClickData(getAPI().getLanguageAPI().replaceColoredKeys(
                                        "$inventories.none.main.items.report.book.entrance_title$\n\n\n\n\n"),
                                        null,
                                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, getAPI().getLanguageAPI().replaceColoredKeys(
                                                "\n$inventories.none.main.items.report.book.entrance_description$\n")),
                                        new ClickEvent(ClickEvent.Action.OPEN_URL, getAPI().getLanguageAPI().replaceColoredKeys(
                                                "\n$inventories.none.main.items.report.book.url$\n"))),
                                new ClickData(getAPI().getLanguageAPI().replaceColoredKeys(
                                        "\n$inventories.none.main.items.report.book.back_title$\n"),
                                        null,
                                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, getAPI().getLanguageAPI().replaceColoredKeys(
                                                "\n$inventories.none.main.items.report.book.back_description$\n")),
                                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nnp"))
                            }
                        });
                    return true;
                }));
    }

    public void onUpdateAsync(GuiUpdate update) {
        // 顶栏
        GuiUtils.setTopBar(update);
        // 上边填充
        for (int i = 9; i < 18; i++) {
            update.setButton(i, "none", "bg_brown");
        }
        // 左侧：玩家设置
        ActionButton playerInfoButton = new ActionButton("info",
                SkullKits.getSkull(update.getPlayer()),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    return true;
                });
        playerInfoButton.init(this);
        update.setButton(18, playerInfoButton);
        update.setButton(19, "mail");
        update.setButton(20, "town");
        update.setButton(27, "title");
        update.setButton(28, "achievement");
        update.setButton(29, "rank");
        update.setButton(36, "task");
        // 左中竖
        update.setButton(21, "none", "bg_blue");
        update.setButton(30, "none", "bg_blue");
        update.setButton(39, "none", "bg_blue");
        // 玩家指南
        update.setButton(22, "guide");
        // 中上下分隔
        update.setButton(31, "none", "bg_lime");
        // 偏好设置
        update.setButton(40, "settings");
        // 左中竖
        update.setButton(23, "none", "bg_yellow");
        update.setButton(32, "none", "bg_yellow");
        update.setButton(41, "none", "bg_yellow");
        // 右侧：附加功能
        update.setButton(24, "toolkit");
        update.setButton(25, "report");
        // 下边填充
        for (int i = 45; i < 54; i++) {
            update.setButton(i, "none", "bg_brown");
        }
    }
}
