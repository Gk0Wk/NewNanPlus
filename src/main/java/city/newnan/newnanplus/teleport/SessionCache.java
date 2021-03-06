package city.newnan.newnanplus.teleport;

import java.util.HashMap;
import java.util.UUID;

public class SessionCache {
    /**
     * 缓存Map
     */
    private final HashMap<UUID, Session> cacheMap = new HashMap<>();

    /**
     * 会话类，内部用
     */
    private static class Session {
        public UUID target;
        public String method;
        public long expiryTime;
        public Session(UUID target, String method, long expiryTime) {
            this.target = target;
            this.method = method;
            this.expiryTime = expiryTime;
        }
    }

    /**
     * 添加一个传送会话
     * @param sourcePlayer 发出请求的玩家的UUID
     * @param targetPlayer 请求的目标玩家的UUID
     * @param method 会话的方法(类型)
     * @param deltaTime 会话有效时长，小于0则不会过期
     */
    public void set(UUID sourcePlayer, UUID targetPlayer, String method, long deltaTime)
    {
        cacheMap.put(sourcePlayer, new Session(targetPlayer, method,
                (deltaTime < 0) ? Long.MAX_VALUE : System.currentTimeMillis() + deltaTime));
    }

    /**
     * 检查一个会话是否存在
     * @param sourcePlayer 发出请求的玩家的UUID
     * @param targetPlayer 请求的目标玩家的UUID，null则不会检测
     * @param method 会话的方法(类型)，null则不会检测，不区分大小写
     * @return 结果码，-1未找到，0找到且过期，否则返回会话剩余时间(毫秒)
     */
    public long test(UUID sourcePlayer, UUID targetPlayer, String method)
    {
        Session session = cacheMap.get(sourcePlayer);

        if (session == null)
            return -1;
        if (targetPlayer != null && targetPlayer != session.target)
            return -1;
        if (method != null && !method.equalsIgnoreCase(session.method))
            return -1;

        long delta = session.expiryTime - System.currentTimeMillis();
        return (delta > 0) ? delta : 0;
    }

    /**
     * 移除一个会话
     * @param sourcePlayer 发出请求的玩家的UUID
     */
    public void remove(UUID sourcePlayer)
    {
        cacheMap.remove(sourcePlayer);
    }
}