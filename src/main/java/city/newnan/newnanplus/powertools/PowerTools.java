package city.newnan.newnanplus.powertools;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.CommandExceptions;
import city.newnan.newnanplus.utility.ItemKit;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PowerTools implements NewNanPlusModule {
    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    public PowerTools() {
        plugin = NewNanPlus.getPlugin();
        plugin.commandManager.register("msg", this);
        plugin.commandManager.register("titlemsg", this);
        plugin.commandManager.register("titlebroadcast", this);
        plugin.commandManager.register("whois", this);
        plugin.commandManager.register("deserializeitem", this);
        plugin.commandManager.register("serializeitem", this);
        plugin.commandManager.register("skull", this);
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {

    }

    /**
     * 执行某个命令
     *
     * @param sender  发送指令者的实例
     * @param command 被执行的指令实例
     * @param token   指令的标识字符串
     * @param args    指令的参数
     */
    @Override
    public void executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String token, @NotNull String[] args) throws Exception {
        switch (token) {
            case "msg":
                sendMessage(args);
                break;
            case "titlemsg":
                sendTitleMessage(args);
                break;
            case "titlebroadcast":
                sendTitleBroadcast(args);
                break;
            case "whois":
                lookupPlayer(sender, args);
                break;
            case "deserializeitem":
                deserializeItem(sender, args);
                break;
            case "serializeitem":
                serializeItem(sender, args);
                break;
            case "skull":
                skullCommand(sender, args);
                break;
        }
    }

    private void sendMessage(String[] args) throws Exception {
        if (args.length < 2) {
            throw new CommandExceptions.BadUsageException();
        }
        // 找到这个玩家，异常将会抛出
        OfflinePlayer player = ((city.newnan.newnanplus.playermanager.PlayerManager)plugin
                .getModule(city.newnan.newnanplus.playermanager.PlayerManager.class)).findOnePlayerByName(args[0]);
        if (!player.isOnline()) {
            throw new CommandExceptions.PlayerOfflineException();
        }
        plugin.messageManager.printf(player.getPlayer(), true, false, StringUtils.join(args, ' ', 1, args.length));
    }

    private void sendTitleMessage(String[] args) throws Exception {
        // 检查参数
        if (args.length < 2 || args.length > 4) {
            throw new CommandExceptions.BadUsageException();
        }
        // 找玩家
        Player player = NewNanPlus.getPlugin().getServer().getPlayer(args[0]);
        if (player == null) {
            throw new CommandExceptions.PlayerNotFountException();
        }

        // 寻找标题
        String title = null;
        String subTitle = null;
        Sound sound = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].matches("^title:.*")) {
                title = ChatColor.translateAlternateColorCodes('&', args[i].replaceFirst("^title:", ""));
            }
            else if (args[i].matches("^subtitle:.*")) {
                subTitle = ChatColor.translateAlternateColorCodes('&', args[i].replaceFirst("^subtitle:", ""));
            }
            else if (args[i].matches("^sound:.*")) {
                sound = Sound.valueOf(args[i].replaceFirst("^sound:", ""));
            }
        }

        if (title != null || subTitle != null)
            player.sendTitle(title, subTitle, 3, 37, 2);
        if (sound != null)
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    private void sendTitleBroadcast(String[] args) throws Exception {
        // 检查参数
        if (args.length < 1 || args.length > 3) {
            throw new CommandExceptions.BadUsageException();
        }

        // 寻找标题
        String title = null;
        String subTitle = null;
        Sound sound = null;
        for (String arg : args) {
            if (arg.matches("^title:.*")) {
                title = ChatColor.translateAlternateColorCodes('&', arg.replaceFirst("^title:", ""));

            }
            else if (arg.matches("^subtitle:.*")) {
                subTitle = ChatColor.translateAlternateColorCodes('&', arg.replaceFirst("^subtitle:", ""));
            }
            else if (arg.matches("^sound:.*")) {
                sound = Sound.valueOf(arg.replaceFirst("^sound:", ""));
            }
        }

        String finalTitle = title;
        String finalSubTitle = subTitle;
        Sound finalSound = sound;
        NewNanPlus.getPlugin().getServer().getOnlinePlayers().forEach(player -> {
            if (!player.hasPermission("newnanplus.powertools.titlebroadcast.bypass")) {
                if (finalTitle != null || finalSubTitle != null)
                    player.sendTitle(finalTitle, finalSubTitle, 3, 37, 2);
                if (finalSound != null)
                    player.playSound(player.getLocation(), finalSound, 1.0f, 1.0f);
            }
        });
    }

    public ItemStack createBook(String title, String author, List<String> lore, BookMeta.Generation generation, List<String> texts) {
        ItemStack aBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) aBook.getItemMeta();
        assert bookMeta != null;

        bookMeta.setTitle(title);
        bookMeta.setAuthor(author);
        bookMeta.setLore(lore);
        bookMeta.setGeneration(generation);
        int linePointer = 1;
        StringBuilder pageBuffer = new StringBuilder();
        List<String> pages = new ArrayList<>();
        for (String text : texts) {
            for (String line : text.split("\n")) {
                linePointer++;
                if (line.equals("---"))
                    pageBuffer.append("===================").append('\n');
                else if (!line.equals("\\p"))
                    pageBuffer.append(line).append('\n');
                if (linePointer == 14 || line.equals("\\p")) {
                    pages.add(pageBuffer.toString());
                    pageBuffer.delete(0, pageBuffer.length());
                    linePointer = 0;
                }
            }
        }
        if (linePointer != 0) {
            pages.add(pageBuffer.toString());
            pageBuffer.delete(0, pageBuffer.length());
        }
        bookMeta.setPages(pages);
        aBook.setItemMeta(bookMeta);
        return aBook;
    }

    private void lookupPlayer(CommandSender sender, String[] args) throws Exception {
        if (args.length != 1) {
            throw new CommandExceptions.BadUsageException();
        }
        List<OfflinePlayer> players = null;
        if (args[0].matches("^name:.*")) {
            String name = args[0].replaceFirst("^name:", "");
            players = Arrays.stream(plugin.getServer().getOfflinePlayers()).
                    filter(oPlayer -> Objects.equals(oPlayer.getName(), name)).collect(Collectors.toList());
        }
        else if (args[0].matches("^uuid:.*")){
            String uuid = args[0].replaceFirst("^uuid:", "");
            for (OfflinePlayer oPlayer : plugin.getServer().getOfflinePlayers()) {
                if (oPlayer.getUniqueId().toString().equals(uuid)) {
                    players = new ArrayList<>();
                    players.add(oPlayer);
                    break;
                }
            }
        } else {
            throw new CommandExceptions.BadUsageException();
        }

        if (players == null || players.size() == 0) {
            throw new CommandExceptions.PlayerNotFountException();
        }

        for (OfflinePlayer player : players) {
            plugin.messageManager.printf(sender, "$module_message.power_tools.user_display_format$",
                    player.getName(), player.getUniqueId().toString());
        }
    }

    private void deserializeItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return;

        Player player = (Player) sender;
        if (args.length !=0) {
            ItemStack itemStack;
            if (args[0].charAt(0) == '{') {
                itemStack = ItemKit.fromJSON(args[0]);
            } else {
                itemStack = ItemKit.fromBase64(args[0]);
            }
            if (itemStack != null) {
                player.getInventory().addItem(itemStack);
            }
        }
    }

    private void serializeItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return;

        Player player = (Player) sender;
        if (args.length > 0 && args[0].equals("base64")) {
            sender.sendMessage(ItemKit.toBase64String(player.getInventory().getItemInMainHand()));
        } else {
            sender.sendMessage(Objects.requireNonNull(ItemKit.toJSON(player.getInventory().getItemInMainHand())));
        }
    }

    /**
     * /nnp skull指令
     * @param sender 指令发送者
     * @param args 指令参数
     * @throws Exception 任何异常
     */
    public static void skullCommand(CommandSender sender, String[] args) throws Exception {
        if (args.length != 1) {
            throw new CommandExceptions.BadUsageException();
        }

        Player player = (Player) sender;
        if (player.getInventory().addItem(ItemKit.getSkull(args[0])).size() > 0) {
            throw new CommandExceptions.CustomCommandException(NewNanPlus.getPlugin().
                    messageManager.sprintf("$global_message.no_more_space_in_inventory$"));
        }

        NewNanPlus.getPlugin().messageManager.printf(sender, "$module_message.power_tools.skull_create_succeed$");
    }
}
