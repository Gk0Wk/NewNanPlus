package city.newnan.newnanplus.utility;

import city.newnan.api.config.ConfigUtil;
import city.newnan.newnanplus.NewNanPlus;

import java.util.*;

import me.lucko.helper.config.ConfigurationNode;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlayerConfig {
    private static NewNanPlus plugin;
    @Deprecated
    private static int[] pluginVersion;

    public static void init(@NotNull NewNanPlus plugin)
    {
        PlayerConfig.plugin = plugin;
        pluginVersion = Arrays.stream(plugin.getDescription().getVersion().split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    private static final HashMap<OfflinePlayer, PlayerConfig> playerConfigs = new HashMap<>();
    public static PlayerConfig getPlayerConfig(@NotNull OfflinePlayer player) throws Exception
    {
        PlayerConfig playerConfig = playerConfigs.get(player);
        if (playerConfig == null) {
            playerConfig = new PlayerConfig(player);
            playerConfigs.put(player, playerConfig);
        }
        return playerConfig;
    }
    public static void unloadPlayerConfig(@NotNull OfflinePlayer player) throws Exception
    {
        PlayerConfig playerConfig = playerConfigs.remove(player);
        if (playerConfig != null)
            playerConfig.commit();
    }

    /**
     * 看看文件的版本情况
     * @param versionCode 文件版本字符串
     * @return 如果文件版本旧于插件版本，就返回-1，如果相同则返回0，如果新于插件版本就返回1
     */
    @Deprecated
    private static int compareVersion(String versionCode)
    {
        int[] fileVersion = Arrays.stream(versionCode.split("\\.")).mapToInt(Integer::parseInt).toArray();
        int length = Math.min(pluginVersion.length, fileVersion.length);
        for (int i = 0; i < length; i++) {
            if (fileVersion[i] == pluginVersion[i])
                continue;
            if (fileVersion[i] < pluginVersion[i])
                return -1;
            else
                return 1;
        }
        if (pluginVersion.length == fileVersion.length) {
            return 0;
        } else {
            return (pluginVersion.length > fileVersion.length) ? -1 : 1;
        }
    }

    private final OfflinePlayer player;
    private final String configFilePath;

    public PlayerConfig(OfflinePlayer player) throws Exception
    {
        this.player = player;
        configFilePath = "player/" + player.getUniqueId().toString() + ".yml";
        ConfigurationNode config = plugin.configManagers.getOrCopyTemplate(configFilePath, "player/template.yml");

        switch (Objects.requireNonNull(config.getNode("version").getString())) {
        case "1.6.0":
        case "1.7.0":
        case "1.7.1":
        case "1.7.2":
        case "1.7.3":
        case "1.7.4":
            config.getNode("emails", "read").setValue(config.getNode("emails", "readed"));
            config.getNode("emails", "readed").setValue(null);
        case "1.7.5":
            lastLoginTime = config.getNode("last-login-time").getLong(0);
            unreadEmails = new ArrayList<>(ConfigUtil.setListIfNull(config.getNode("emails", "unread")).getList(Object::toString));
            readEmails = new ArrayList<>(ConfigUtil.setListIfNull(config.getNode("emails", "read")).getList(Object::toString));
            loginTaskQueue = new ArrayList<>(ConfigUtil.setListIfNull(config.getNode("login-task-queue")).getList(Object::toString));
            tpaBlockList = new ArrayList<>(ConfigUtil.setListIfNull(config.getNode("tpa-blocklist")).getList(Object::toString));
        }
        commit();
    }

    public OfflinePlayer getPlayer() { return player; }
    public String getConfigFilePath() { return configFilePath; }

    private long lastLoginTime;
    public void setLastLoginTime(long time) { this.lastLoginTime = time; }
    public long getLastLoginTime() { return this.lastLoginTime; }

    private List<String> unreadEmails, readEmails;
    public List<String> getUnreadEmails() { return this.unreadEmails; }
    public List<String> getReadEmails() { return this.readEmails; }
    public void setUnreadEmails(List<String> mails) { this.unreadEmails = mails; }
    public void setReadEmails(List<String> mails) { this.readEmails = mails; }

    private List<String> loginTaskQueue;
    public List<String> getLoginTaskQueue() { return this.loginTaskQueue; }
    public void setLoginTaskQueue(List<String> tasks) { this.loginTaskQueue = tasks; }

    private List<String> tpaBlockList;
    public List<String> getTpaBlockList() { return this.tpaBlockList; }
    public void setTpaBlockList(List<String> list) { this.tpaBlockList = list; }

    /**
     * 保存更改
     */
    public void commit() throws Exception
    {
        ConfigurationNode config = plugin.configManagers.getOrCopyTemplate(configFilePath, "player/template.yml");

        config.getNode("version").setValue(plugin.getDescription().getVersion());
        config.getNode("name").setValue(player.getName());
        config.getNode("last-login-time").setValue(lastLoginTime);
        config.getNode("emails", "unread").setValue(unreadEmails);
        config.getNode("emails", "read").setValue(readEmails);
        config.getNode("login-task-queue").setValue(loginTaskQueue);
        config.getNode("tpa-blocklist").setValue(tpaBlockList);

        plugin.configManagers.save(configFilePath);
    }
}
