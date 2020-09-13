package city.newnan.NewNanPlus.Utility;

import java.sql.*;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * NewNanCity 通用库 MySQL
 * 提供MySQL数据库连接、表操作、查询和更新功能。
 */
public class NewNanSQL {
    /**
     * 数据库连接
     */
    private Connection Connection;
    /**
     * Bukkit 插件实例
     */
    private final Plugin Plugin;
    /**
     * 数据库程序所在的Host地址
     */
    private final String Host;
    /**
     * 数据库程序的监听端口
     */
    private final int Port;
    /**
     * 数据库名称
     */
    private final String Database;
    /**
     * 登录用户名
     */
    private final String Username;
    /**
     * 登录密码
     */
    private final String Password;
    /**
     * 数据库连接参数
     */
    private final Map<String, String> Parameters;
    /**
     * 缓存的查询结果
     */
    private final Map<PreparedStatement, ResultSet> Results = new HashMap<>();

    /**
     * 构造方法
     * @param plugin 插件实例
     * @param host 数据库程序所在的Host地址
     * @param port 数据库程序监听端口
     * @param database 数据库名称
     * @param username 登录用户名
     * @param password 登录密码
     * @param parameters 数据库连接参数
     */
    public NewNanSQL(Plugin plugin, String host, int port, String database, String username, String password, Map<String, String> parameters) {
        Plugin = plugin;
        Host = host;
        Port = port;
        Database = database;
        Username = username;
        Password = password;
        Parameters = parameters;

        // 为什么要这么做：https://blog.csdn.net/yanwushu/article/details/7574713?utm_medium=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-1.channel_param&depth_1-utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-1.channel_param
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 构造方法，使用默认端口3306
     * @param plugin 插件实例
     * @param host 数据库程序所在的Host地址
     * @param database 数据库名称
     * @param username 登录用户名
     * @param password 登录密码
     * @param parameters 数据库连接参数
     */
    public NewNanSQL(Plugin plugin, String host, String database, String username, String password, Map<String, String> parameters) {
        this(plugin, host, 3306, database, username, password, parameters);
    }

    /**
     * 构造方法，不带参数
     * @param plugin 插件实例
     * @param host 数据库程序所在的Host地址
     * @param port 数据库程序监听端口
     * @param database 数据库名称
     * @param username 登录用户名
     * @param password 登录密码
     */
    public NewNanSQL(Plugin plugin, String host, int port, String database, String username, String password) {
        this(plugin, host, port, database, username, password, new TreeMap<>());
    }

    /**
     * 构造方法，不带参数并使用默认端口欧3306
     * @param plugin 插件实例
     * @param host 数据库程序所在的Host地址
     * @param database 数据库名称
     * @param username 登录用户名
     * @param password 登录密码
     */
    public NewNanSQL(Plugin plugin, String host, String database, String username, String password) {
        this(plugin, host, 3306, database, username, password, new TreeMap<>());
    }

    /**
     * 同步建立数据库连接
     */
    public void openConnectionSync() {
        try {
            synchronized (this) {
                if (Connection != null && !Connection.isClosed()) {
                    return;
                }

                // URL生成
                StringBuilder url = new StringBuilder();
                AtomicBoolean ifFirst = new AtomicBoolean(true);
                url.append("jdbc:mysql://").append(Host).append(":").append(Port).append("/").append(Database);
                Parameters.forEach((key, value) -> {
                    if (ifFirst.get()) {
                        url.append("?");
                        ifFirst.set(false);
                    } else {
                        url.append("&");
                    }
                    url.append(key).append("=").append(value);
                });
                Connection = DriverManager.getConnection(url.toString(), Username, Password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 异步建立数据库连接
     */
    public void openConnectionAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(Plugin, this::openConnectionSync);
    }

    /**
     * 同步执行数据库更新语句(表操作、属性操作、字段操作)
     * @param preparedStatement 预构造的更新语句
     */
    public void executeUpdateSync(PreparedStatement preparedStatement) {
        try {
            if (!Connection.isValid(0)) {
                openConnectionSync();
            }
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 异步执行数据库更新语句(表操作、属性操作、字段操作)
     * @param preparedStatement 预构造的更新语句
     */
    public void executeUpdateAsync(PreparedStatement preparedStatement) {
        Bukkit.getScheduler().runTaskAsynchronously(Plugin, () -> {
            executeUpdateSync(preparedStatement);
        });
    }

    /**
     * 同步执行数据库更新语句(表操作、属性操作、字段操作)
     * @param sql 更新语句
     */
    public void executeUpdateSync(String sql) {
        try {
            executeUpdateSync(Connection.prepareStatement(sql));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 异步执行数据库更新语句(表操作、属性操作、字段操作)
     * @param sql 更新语句
     */
    public void executeUpdateAsync(String sql) {
        try {
            executeUpdateAsync(Connection.prepareStatement(sql));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得预构造语句
     * @param sql SQL语句
     * @return 预构造语句
     */
    public PreparedStatement prepareStatement(String sql) {
        try {
            return Connection.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 执行一次性查询，不缓存查询结果
     * @param preparedStatement 预构造的查询
     * @return 查询结果
     */
    public ResultSet executeQueryOneTime(PreparedStatement preparedStatement) {
        try {
            if (!Connection.isValid(0)) {
                openConnectionSync();
            }
            ResultSet result = preparedStatement.executeQuery();
            preparedStatement.close();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 执行查询，并缓存查询结果
     * @param preparedStatement 预构造的查询
     * @return 查询结果
     */
    public ResultSet executeQuery(PreparedStatement preparedStatement) {
        ResultSet result = executeQueryOneTime(preparedStatement);
        Results.put(preparedStatement, result);
        return result;
    }

    /**
     * 获得查询语句先前的查询缓存，如果没有，就执行查询并缓存
     * @param preparedStatement 预构造的查询
     * @return 查询结果
     */
    public ResultSet getQueryResult(PreparedStatement preparedStatement) {
        if (Results.containsKey(preparedStatement)) {
            return Results.get(preparedStatement);
        } else {
            return executeQuery(preparedStatement);
        }
    }

    /**
     * 获得查询语句的缓存，并将其从缓存池中删除
     * @param preparedStatement 预构造的查询
     * @return 查询结果上一次的缓存结果，如果没有缓存就返回null
     */
    public ResultSet popResult(PreparedStatement preparedStatement) {
        if (Results.containsKey(preparedStatement)) {
            ResultSet result = Results.get(preparedStatement);
            Results.remove(preparedStatement);
            return result;
        } else {
            return null;
        }
    }

    /**
     * 清除所有的查询缓存
     */
    public void clearResults() {
        Results.clear();
    }

    /**
     * 获得注册该模块的插件实例
     * @return 注册该模块的插件实例
     */
    public Plugin getPlugin() {
        return Plugin;
    }

    /**
     * 获得数据库连接实例
     * @return 数据库连接实例
     */
    public Connection getConnection() {
        return Connection;
    }

    /**
     * 获得数据库软件所在Host的地址
     * @return 数据库软件所在Host的地址
     */
    public String getHost() {
        return Host;
    }

    /**
     * 获得数据库软件监听端口
     * @return 数据库软件监听端口
     */
    public int getPort() {
        return Port;
    }

    /**
     * 获得数据库名称
     * @return 数据库名称
     */
    public String getDatabase() {
        return Database;
    }

    /**
     * 获得登录用户名
     * @return 登录用户名
     */
    public String getUsername() {
        return Username;
    }

    /**
     * 获得登录密码
     * @return 登录密码
     */
    public String getPassword() {
        return Password;
    }

    /**
     * 获得数据库连接参数
     * @return 数据库连接参数
     */
    public Map<String, String> getParameters() {
        return Parameters;
    }
}
