package city.newnan.newnanplus.playermanager;

import city.newnan.newnanplus.NewNanPlus;
import city.newnan.newnanplus.NewNanPlusModule;
import city.newnan.newnanplus.exception.ModuleExeptions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MailSystem implements NewNanPlusModule {

    /**
     * 插件的唯一静态实例，加载不成功是null
     */
    private final NewNanPlus plugin;

    private final Map<String, Mail> mails = new HashMap<>();
    public Mail getMail(String mailName) {
        return mails.get(mailName);
    }

    public MailSystem() throws Exception {
        plugin = NewNanPlus.getPlugin();
        if (!plugin.configManager.get("config.yml").getBoolean("module-mail.enable", false)) {
            throw new ModuleExeptions.ModuleOffException();
        }
        reloadConfig();
    }

    /**
     * 重新加载模块的配置
     */
    @Override
    public void reloadConfig() {
        mails.clear();
        File mailDir = new File(plugin.getDataFolder(), "email/");
        if (mailDir.exists() && mailDir.isDirectory()) {
            for (String fileName : mailDir.list()) {
                File mailFile = new File(mailDir, fileName);
                if (!mailFile.isFile() || !mailFile.exists() || !fileName.endsWith(".yml")) {
                    continue;
                }
                mails.put(fileName.split("\\.", 2)[0], new Mail(plugin.configManager.get("email/" + fileName)));
            }
        }
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

    }

    /**
     * 为某玩家打开某封信
     * @param player 玩家实例
     * @param mail 信件实例
     * @param hasRead 玩家是否已经阅读过这封信，阅读过就无法再执行指令和领取物品了
     * @return 如果true说明玩家完成阅读，false说明需要回滚到未读状态
     */
    public boolean showEmail(Player player, Mail mail, boolean hasRead) {
        boolean readIt;
        List<String> texts = new ArrayList<>();

        try {
            // 邮件头渲染
            texts.add("§l" + mail.title + "§r");
            texts.add("§4" + mail.author + "§r");
            texts.add("§7" + mail.date + "§r\n");

            // 权限检查
            if (mail.permission != null && !player.hasPermission(mail.permission)) {
                if (mail.permissionMessage != null) {
                    texts.add(ChatColor.translateAlternateColorCodes('&', mail.permissionMessage));
                } else {
                    texts.add(plugin.wolfyLanguageAPI.
                            replaceColoredKeys("$module_message.player_manager.email_no_permission$"));
                }
                throw new Exception("Permission denied.");
            }

            // action解析和执行 首先判断是否已读和是否有action
            if (!hasRead && mail.actions.size() != 0) {
                // 过期检查
                if (mail.expiry != 0 && mail.expiry < System.currentTimeMillis()) {
                    // 过期则不执行命令，并显示邮件已失效
                    texts.add(plugin.wolfyLanguageAPI.replaceColoredKeys(
                            "$module_message.player_manager.email_outdated$") + "§r\n");
                } else {
                    // 检查背包是否有足够的空间来存储附件
                    if (inventoryTouch(player.getInventory(), mail.actions)) {
                        CommandSender sender = plugin.getServer().getConsoleSender();
                        mail.actions.forEach(action -> {
                            if (action.type == Mail.MailAction.ActionType.ITEM) {
                                player.getInventory().addItem(action.item);
                            } else if (action.type == Mail.MailAction.ActionType.COMMAND) {
                                plugin.getServer().dispatchCommand(sender,
                                        action.command.replaceAll("@s", player.getName()));
                            }
                        });
                    } else {
                        // 命令需要玩家物品栏有空位置,否则退回
                        texts.add(plugin.wolfyLanguageAPI.replaceColoredKeys
                                ("$module_message.player_manager.email_require_inventory$"));
                        throw new Exception("Inventory full.");
                    }
                }
            }

            texts.add("===================");
            // 正文渲染
            texts.add(mail.text);

            readIt = true;
        } catch (Exception e) {
            e.printStackTrace();
            readIt = false;
        }

        player.openBook(((city.newnan.newnanplus.powertools.PowerTools)
                plugin.getModule(city.newnan.newnanplus.powertools.PowerTools.class))
                .createBook(mail.title, mail.author, null, BookMeta.Generation.ORIGINAL, texts));
        return readIt;
    }

    private boolean inventoryTouch(Inventory originInventory, List<Mail.MailAction> actions) {
        if (actions == null || actions.isEmpty())
            return true;

        // 拷贝背包
        Inventory inventory = plugin.getServer().createInventory(null, originInventory.getType());
        ListIterator<ItemStack> oriIter =  originInventory.iterator();
        int slotIndex = 0;
        while (oriIter.hasNext()) {
            ItemStack item = oriIter.next();
            if (item != null) {
                inventory.setItem(slotIndex, item.clone());
            }
            slotIndex ++;
        }

        // 尝试填充
        AtomicBoolean canFill = new AtomicBoolean(true);
        actions.stream().filter(action -> action.type == Mail.MailAction.ActionType.ITEM).forEach(action -> {
            if (!canFill.get())
                return;
            if (!inventory.addItem(action.item).isEmpty()) {
                canFill.set(false);
            }
        });

        inventory.clear();
        return canFill.get();
    }

}
