package city.newnan.newnanplus.playermanager;

import city.newnan.newnanplus.utility.ItemKit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import me.lucko.helper.config.ConfigurationNode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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
    public Mail(ConfigurationNode mailNode)
    {
        title = mailNode.getNode("title").getString("[No Title]");
        author = mailNode.getNode("author").getString("[Unknown Author]");
        date = mailNode.getNode("date").getString("[Unknown Date]");

        String _permission = mailNode.getNode("permission").getString(null);
        permission = (_permission == null || _permission.isEmpty()) ? null : _permission;

        if (permission != null) {
            String _permissionMessage = mailNode.getNode("permission-message").getString(null);
            permissionMessage = (_permissionMessage == null || _permissionMessage.isEmpty()) ? null : _permissionMessage;
        } else {
            permissionMessage = null;
        }

        {
            String availableDateString = mailNode.getNode("available-until").getString(null);
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
        mailNode.getNode("commands").getList(Object::toString).forEach(actionString -> {
            Mail.MailAction action = Mail.MailAction.parse(actionString);
            if (action != null)
                actions.add(action);
        });

        text = mailNode.getNode("text").getString("[No Text]");

        String _icon = mailNode.getNode("icon").getString(null);
        ItemStack icon1;
        if (_icon == null || _icon.isEmpty()) {
            icon1 = defaultIcon;
        } else {
            try {
                icon1 = new ItemStack(Material.valueOf(_icon.toUpperCase()));
            } catch (IllegalArgumentException e1) {
                try {
                    icon1 = ItemKit.getSkull(_icon);
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

        public static MailAction parse(String actionString)
        {
            String[] splits = actionString.split(" ", 2);
            if (splits.length < 2)
                return null;
            if (splits[0].equalsIgnoreCase("ITEM")) {
                MailAction action = new MailAction();
                action.type = ActionType.ITEM;
                // 两种不同的构造方式。{开头的是JSON格式，反之是base64格式
                if (splits[1].charAt(0) == '{') {
                    action.item = ItemKit.fromJSON(splits[1]);
                } else {
                    action.item = ItemKit.fromBase64(splits[1]);
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

        enum ActionType { ITEM,
            COMMAND }
    }
}
