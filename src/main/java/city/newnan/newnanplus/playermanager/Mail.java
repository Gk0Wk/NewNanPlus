package city.newnan.newnanplus.playermanager;

import me.wolfyscript.utilities.api.utils.inventory.ItemUtils;
import me.wolfyscript.utilities.api.utils.inventory.PlayerHeadUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Mail {
    public final String title;
    public final String author;
    public final String date;
    public long expiry;
    public final String permission;
    public final String permissionMessage;
    public final List<MailAction> actions;
    public final ItemStack icon;
    public final String text;

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final ItemStack defaultIcon = new ItemStack(Material.WRITTEN_BOOK);
    public Mail(ConfigurationSection config) {

        title = config.getString("title", "[No Title]");
        author = config.getString("author", "[Unknown Author]");
        date = config.getString("date", "[Unknown Date]");

        String _permission = config.getString("permission", null);
        permission = (_permission == null || _permission.isEmpty()) ? null : _permission;

        if (permission != null) {
            String _permissionMessage = config.getString("permission-message", null);
            permissionMessage = (_permissionMessage == null || _permissionMessage.isEmpty()) ? null : _permissionMessage;
        } else {
            permissionMessage = null;
        }

        {
            String availableDateString = config.getString("available-until");
            if (availableDateString != null) {
                try {
                    expiry = dateFormatter.parse(availableDateString).getTime();
                } catch (Exception e) {
                    expiry = 0;
                }
            } else {
                expiry = 0;
            }
        }

        actions = new ArrayList<>();
        config.getStringList("commands").forEach(actionString -> {
            Mail.MailAction action = Mail.MailAction.parse(actionString);
            if (action != null)
                actions.add(action);
        });

        text = config.getString("text", "[No Text]");

        String _icon = config.getString("icon", null);
        ItemStack icon1;
        if (_icon == null || _icon.isEmpty()) {
            icon1 = defaultIcon;
        } else {
            try {
                icon1 = new ItemStack(Material.valueOf(_icon.toUpperCase()));
            } catch (IllegalArgumentException e1) {
                try {
                    icon1 = PlayerHeadUtils.getViaValue(_icon);
                } catch (Exception e2) {
                    icon1 = defaultIcon;
                }
            }
        }
        icon = icon1;
    }

    static class MailAction {
        public ActionType type;
        public String command;
        public ItemStack item;

        public static MailAction parse(String actionString) {
            String[] splits = actionString.split(" ", 2);
            if (splits.length < 2)
                return null;
            if (splits[0].equalsIgnoreCase("ITEM")) {
                MailAction action = new MailAction();
                action.type = ActionType.ITEM;
                // 两种不同的构造方式。{开头的是JSON格式，反之是base64格式
                if (splits[1].charAt(0) == '{') {
                    action.item = ItemUtils.convertJsontoItemStack(splits[1]);
                } else {
                    action.item = ItemUtils.deserializeNMSItemStack(splits[1]);
                }
                return action;
            } else if (splits[0].equalsIgnoreCase("COMMAND")) {
                MailAction action = new MailAction();
                action.type = ActionType.COMMAND;
                action.command = splits[1];
                return action;
            }
            return null;
        }

        enum ActionType {ITEM, COMMAND}
    }
}
