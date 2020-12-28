package city.newnan.newnanplus.maingui;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.utility.PlayerConfig;
import me.wolfyscript.utilities.api.inventory.GuiHandler;
import me.wolfyscript.utilities.api.inventory.GuiUpdate;
import me.wolfyscript.utilities.api.inventory.InventoryAPI;
import me.wolfyscript.utilities.api.inventory.button.ButtonState;
import me.wolfyscript.utilities.api.inventory.button.buttons.ActionButton;
import me.wolfyscript.utilities.api.inventory.button.buttons.DummyButton;
import me.wolfyscript.utilities.api.inventory.button.buttons.ToggleButton;
import me.wolfyscript.utilities.api.utils.inventory.PlayerHeadUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class GuiUtils {
    private static InventoryAPI<GuiCache> inventoryAPI;
    private static NewNanPlus plugin;
    private static Field historyField;

    private static ToggleButton helpButton;
    private static final List<ItemStack> pageNumberButton = new ArrayList<>();

    public static void init(NewNanPlus plugin) throws NoSuchFieldException {
        GuiUtils.plugin = plugin;
        inventoryAPI = plugin.inventoryAPI;

        historyField = GuiHandler.class.getDeclaredField("clusterHistory");
        historyField.setAccessible(true);

        inventoryAPI.getOrRegisterGuiCluster("none");

        // 背景玻璃板
        inventoryAPI.registerButton("none", new DummyButton("bg_white", new ButtonState("none", "background", Material.WHITE_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_orange", new ButtonState("none", "background", Material.ORANGE_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_magenta", new ButtonState("none", "background", Material.MAGENTA_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_lightblue", new ButtonState("none", "background", Material.LIGHT_BLUE_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_yellow", new ButtonState("none", "background", Material.YELLOW_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_lime", new ButtonState("none", "background", Material.LIME_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_pink", new ButtonState("none", "background", Material.PINK_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_gray", new ButtonState("none", "background", Material.GRAY_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_lightgray", new ButtonState("none", "background", Material.LIGHT_GRAY_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_cyan", new ButtonState("none", "background", Material.CYAN_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_purple", new ButtonState("none", "background", Material.PURPLE_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_blue", new ButtonState("none", "background", Material.BLUE_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_brown", new ButtonState("none", "background", Material.BROWN_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_green", new ButtonState("none", "background", Material.GREEN_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_red", new ButtonState("none", "background", Material.RED_STAINED_GLASS_PANE, 8999)));
        inventoryAPI.registerButton("none", new DummyButton("bg_black", new ButtonState("none", "background", Material.BLACK_STAINED_GLASS_PANE, 8999)));

        // 上一页
        inventoryAPI.registerButton("none", new ActionButton("back",
                PlayerHeadUtils.getViaURL("864f779a8e3ffa231143fa69b96b14ee35c16d669e19c75fd1a7da4bf306c"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    guiHandler.openPreviousInv();
                    return true;
                }));
        // 主页
        inventoryAPI.registerButton("none", new ActionButton("home",
                PlayerHeadUtils.getViaURL("c5a35b5ca15268685c4660535e5883d21a5ec57c55d397234269acb5dc2954f"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    clearHistory(guiHandler, guiHandler.getCurrentGuiCluster());
                    guiHandler.openCluster(guiHandler.getCurrentGuiCluster());
                    return true;
                }));
        // 帮助
        inventoryAPI.registerButton("none", helpButton = new ToggleButton("help", true,
                new ButtonState("help_on",
                        PlayerHeadUtils.getViaURL("5359d91277242fc01c309accb87b533f1929be176ecba2cde63bf635e05e699b"),
                        (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                            NewNanPlus.getPlugin().messageManager.printINFO("[set to false]");
                            guiHandler.setHelpEnabled(false);
                            try {
                                PlayerConfig playerConfig = PlayerConfig.getPlayerConfig(player);
                                playerConfig.setGuiHelp(false);
                                playerConfig.commit();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return true;
                        }),
                new ButtonState("help_off",
                        PlayerHeadUtils.getViaURL("26e1191ddfc694157bb093d9997ee1b677409ed9cdeb856a33aa6f437e58fd2"),
                        (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                            NewNanPlus.getPlugin().messageManager.printINFO("[set to true]");
                            guiHandler.setHelpEnabled(true);
                            try {
                                PlayerConfig playerConfig = PlayerConfig.getPlayerConfig(player);
                                playerConfig.setGuiHelp(true);
                                playerConfig.commit();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                )));
        // 关闭
        inventoryAPI.registerButton("none", new ActionButton("close",
                PlayerHeadUtils.getViaURL("16c60da414bf037159c8be8d09a8ecb919bf89a1a21501b5b2ea75963918b7b"),
                (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                    guiHandler.close();
                    return true;
                }));

        String[] urls = new String[]{
                "6d68343bd0b129de93cc8d3bba3b97a2faa7ade38d8a6e2b864cd868cfab",
                "d2a6f0e84daefc8b21aa99415b16ed5fdaa6d8dc0c3cd591f49ca832b575",
                "96fab991d083993cb83e4bcf44a0b6cefac647d4189ee9cb823e9cc1571e38",
                "cd319b9343f17a35636bcbc26b819625a9333de3736111f2e932827c8e749",
                "d198d56216156114265973c258f57fc79d246bb65e3c77bbe8312ee35db6",
                "7fb91bb97749d6a6eed4449d23aea284dc4de6c3818eea5c7e149ddda6f7c9",
                "9c613f80a554918c7ab2cd4a278752f151412a44a73d7a286d61d45be4eaae1",
                "9e198fd831cb61f3927f21cf8a7463af5ea3c7e43bd3e8ec7d2948631cce879",
                "84ad12c2f21a1972f3d2f381ed05a6cc088489fcfdf68a713b387482fe91e2",
                "9f7aa0d97983cd67dfb67b7d9d9c641bc9aa34d96632f372d26fee19f71f8b7",
                "b0cf9794fbc089dab037141f67875ab37fadd12f3b92dba7dd2288f1e98836",
                "3997e7c194c4702cd214428e1f5e64615726a52f7c6e3a337893091e786722a",
                "7e14f14f1e12ea72575f68134bb4f2b9ec6ce6205525bfc4c62654c55dae547",
                "ed3d5a31819af5665e1ce396bbf8f1e4d98ffd18222da46fadb61cf79562f8",
                "edc3c228dc17254124b6be51f5cb26d08f89727ad27463ff9c4bc29918e1ab",
                "f6bec38d26c02f43dcbf9b1d48b34f1bc4737a6938f2664d4e764272a9b39b61",
                "42d9786a312cb0b5167312f43d747150e7eb528c3d6e9dd27438507dd979a7f3",
                "7be20edf7c2ee65251f771d8673d5ba72adf8945d3eb27d79b9ba97407f76",
                "1beb64cf826831eca246f12c3d397f6881decf98ade887e6bc01ab54263128",
                "1f4de1282fbe384975d91c7ec4e2df2ff17c9da4642bb4ae36af4541a4987b16",
                "f7b29a1bb25b2ad8ff3a7a38228189c9461f457a4da98dae29384c5c25d85",
                "20f0b365d920628c513a6181283411fd85281f879b9761ae64d9b2a8b11bf8",
                "404d561e3bd9e416114782975f50eee7771c251e5c6d3113c8674fdc8ffdf60",
                "359de6e901358903b217dc85fb3bd92d650698978d0b3e9129223432946db",
                "994891a593b030c4fe608f653fb9760d4ffab13b8fc78ed1af46d6f55a92516",
                "76d7299cdef08e5b51b6dcb77dcfcff4267197a7199edd908c5a41783fe73141"
        };

        for (int i = 0; i < urls.length; i++) {
            ItemStack pageNumber = PlayerHeadUtils.getViaURL(urls[i]);
            SkullMeta meta = (SkullMeta) pageNumber.getItemMeta();
            assert meta != null;
            meta.setDisplayName(MessageFormat.format(
                    plugin.wolfyLanguageAPI.replaceColoredKeys("$global_message.page_number$"), i));
            pageNumber.setItemMeta(meta);
            pageNumberButton.add(pageNumber);
        }
    }

    public static void clearHistory(GuiHandler<?> guiHandler, String clusterID) {
        try {
            ((List<?>) ((Map<?,?>) historyField.get(guiHandler)).get(clusterID)).clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isHistoryEmpty(GuiHandler<?> guiHandler, String clusterID) {
        try {
            return ((List<?>) ((Map<?,?>) historyField.get(guiHandler)).get(clusterID)).size() < 2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void setTopBar(GuiUpdate update) {
        // 上一页按钮
        if (isHistoryEmpty(update.getGuiHandler(), update.getGuiHandler().getCurrentGuiCluster())) {
            update.setButton(0, "none", "bg_black");
        } else {
            update.setButton(0, "none", "back");
        }
        // 主页按钮
        if (plugin.inventoryAPI.getGuiCluster(update.getGuiHandler().getCurrentGuiCluster()).getMainMenu()
                .equals(Objects.requireNonNull(update.getGuiHandler().getCurrentInv()).getNamespace())) {
            update.setButton(1, "none", "bg_black");
        } else {
            update.setButton(1, "none", "home");
        }
        // 顶栏填充
        for (int i = 2; i < 7; i++) {
            update.setButton(i, "none", "bg_black");
        }
        // 帮助按钮
        boolean help = true;
        try {
            help = PlayerConfig.getPlayerConfig(update.getPlayer()).getGuiHelp();
        } catch (Exception e) {
            e.printStackTrace();
        }
        helpButton.setState(update.getGuiHandler(), help);
        update.getGuiHandler().setHelpEnabled(help);
        NewNanPlus.getPlugin().messageManager.printINFO("help: " + help);
        update.setButton(7, helpButton);
        // 关闭按钮
        update.setButton(8, "none", "close");
    }

    public static void setListPage(GuiUpdate update, Listable window) {
        int index = window.getPageIndex(update.getGuiHandler());
        int pageCount = window.getPageCount(update.getGuiHandler());

        // 翻到第一页、翻到上一页
        if (index == 0) {
            update.setButton(45, "none", "bg_brown");
            update.setButton(47, "none", "bg_brown");
        } else {
            ActionButton firstPageButton = new ActionButton("first_page",
                    PlayerHeadUtils.getViaURL("118a2dd5bef0b073b13271a7eeb9cfea7afe8593c57a93821e43175572461812"),
                    (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                        window.setPageIndex(0, guiHandler);
                        return true;
                    });
            firstPageButton.init("none", inventoryAPI.getWolfyUtilities());
            update.setButton(45, firstPageButton);

            ActionButton previousPageButton = new ActionButton("previous_page",
                    PlayerHeadUtils.getViaURL("864f779a8e3ffa231143fa69b96b14ee35c16d669e19c75fd1a7da4bf306c"),
                    (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                        window.setPageIndex(index - 1, guiHandler);
                        return true;
                    });
            previousPageButton.init("none", inventoryAPI.getWolfyUtilities());
            update.setButton(47, previousPageButton);
        }

        update.setButton(46, "none", "bg_brown");
        update.setButton(48, "none", "bg_brown");

        if (index >= pageNumberButton.size()) {
            ItemStack pageNumber = PlayerHeadUtils.getViaURL("979a465183a3ba63fe6ae272bc1bf1cd15f2c209ebbfcc5c521b9514682a43");
            SkullMeta meta = (SkullMeta) pageNumber.getItemMeta();
            assert meta != null;
            meta.setDisplayName(MessageFormat.format(
                    plugin.wolfyLanguageAPI.replaceColoredKeys("$global_message.page_number$"), index + 1));
            pageNumber.setItemMeta(meta);
            update.setItem(49, pageNumber);
        } else {
            update.setItem(49, pageNumberButton.get(index + 1));
        }

        update.setButton(50, "none", "bg_brown");
        update.setButton(52, "none", "bg_brown");

        // 翻到最后页、翻到下一页
        if ((pageCount - index) <= 1) {
            update.setButton(51, "none", "bg_brown");
            update.setButton(53, "none", "bg_brown");
        } else {
            ActionButton lastPageButton = new ActionButton("last_page",
                    PlayerHeadUtils.getViaURL("d99f28332bcc349f42023c29e6e641f4b10a6b1e48718cae557466d51eb922"),
                    (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                        window.setPageIndex(0, guiHandler);
                        return true;
                    });
            lastPageButton.init("none", inventoryAPI.getWolfyUtilities());
            update.setButton(53, lastPageButton);

            ActionButton nextPageButton = new ActionButton("next_page",
                    PlayerHeadUtils.getViaURL("d9eccc5c1c79aa7826a15a7f5f12fb40328157c5242164ba2aef47e5de9a5cfc"),
                    (guiHandler, player, inventory, i, inventoryClickEvent) -> {
                        window.setPageIndex(index - 1, guiHandler);
                        return true;
                    });
            nextPageButton.init("none", inventoryAPI.getWolfyUtilities());
            update.setButton(51, nextPageButton);
        }

        AtomicInteger slot = new AtomicInteger(9);
        window.getPage(update.getGuiHandler()).forEach(button -> {
            if (slot.get() > 44)
                return;
            update.setButton(slot.get(), button);
            slot.getAndIncrement();
        });
    }
}
