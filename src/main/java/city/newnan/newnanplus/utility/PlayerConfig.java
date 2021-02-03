package city.newnan.newnanplus.utility;

import city.newnan.newnanplus.NewNanPlus;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class PlayerConfig {
    private static NewNanPlus plugin;
    @Deprecated
    private static int[] pluginVersion;

    public static void init(@NotNull NewNanPlus plugin) {
        PlayerConfig.plugin = plugin;
        pluginVersion = Arrays.stream(plugin.getDescription().getVersion().split("\\.")).
                mapToInt(Integer::parseInt).toArray();
    }

    private static final HashMap<OfflinePlayer, PlayerConfig> playerConfigs = new HashMap<>();
    public static PlayerConfig getPlayerConfig(@NotNull OfflinePlayer player) throws Exception {
        PlayerConfig playerConfig = playerConfigs.get(player);
        if (playerConfig == null) {
            playerConfig = new PlayerConfig(player);
            playerConfigs.put(player, playerConfig);
        }
        return playerConfig;
    }
    public static void unloadPlayerConfig(@NotNull OfflinePlayer player) throws Exception {
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
    private static int compareVersion(String versionCode) {
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

    public PlayerConfig(OfflinePlayer player) throws Exception {
        this.player = player;
        configFilePath = "player/" + player.getUniqueId().toString() + ".yml";
        FileConfiguration config = plugin.configManager.getOrCopyTemplate(configFilePath, "player/template.yml");

        switch (Objects.requireNonNull(config.getString("version"))) {
            case "1.6.0":
            case "1.7.0":
            case "1.7.1":
            case "1.7.2":
            case "1.7.3":
            case "1.7.4":
                config.set("emails.read", config.getStringList("emails.readed"));
                config.set("emails.readed", null);
            case "1.7.5":
                lastLoginTime = config.getLong("last-login-time", 0);
                unreadEmails = config.getStringList("emails.unread");
                readEmails = config.getStringList("emails.read");
                loginTaskQueue = config.getStringList("login-task-queue");
                tpaBlockList = config.getStringList("tpa-blocklist");
        }
        commit();
    }

    public OfflinePlayer getPlayer() {return player;}
    public String getConfigFilePath() {return configFilePath;}

    private long lastLoginTime;
    public void setLastLoginTime(long time) {this.lastLoginTime = time;}
    public long getLastLoginTime() {return this.lastLoginTime;}

    private List<String> unreadEmails, readEmails;
    public List<String> getUnreadEmails() {return this.unreadEmails;}
    public List<String> getReadEmails() {return this.readEmails;}
    public void setUnreadEmails(List<String> mails) {this.unreadEmails = mails;}
    public void setReadEmails(List<String> mails) {this.readEmails = mails;}

    private List<String> loginTaskQueue;
    public List<String> getLoginTaskQueue() {return this.loginTaskQueue;}
    public void setLoginTaskQueue(List<String> tasks) {this.loginTaskQueue = tasks;}

    private List<String> tpaBlockList;
    public List<String> getTpaBlockList() {return this.tpaBlockList;}
    public void setTpaBlockList(List<String> list) {this.tpaBlockList = list;}

    /**
     * 保存更改
     */
    public void commit() throws Exception {
        FileConfiguration config = plugin.configManager.getOrCopyTemplate(configFilePath, "player/template.yml");

        config.set("version", plugin.getDescription().getVersion());
        config.set("name", player.getName());
        config.set("last-login-time", lastLoginTime);
        config.set("emails.unread", unreadEmails);
        config.set("emails.read", readEmails);
        config.set("login-task-queue", loginTaskQueue);
        config.set("tpa-blocklist", tpaBlockList);

        plugin.configManager.save(configFilePath);
    }
}
