package city.newnan.newnanplus.feefly;

/**
 * 飞行玩家
 */
public class FlyingPlayer {
    /**
     * 玩家开始飞行的时间，毫秒时间戳
     */
    public long flyStartTimestamp;
    /**
     * 玩家飞行之前的速度
     */
    public float previousFlyingSpeed;

    /**
     * 构造函数
     * @param timestamp 玩家开始飞行的时间，毫秒时间戳
     * @param speed 玩家飞行之前的速度
     */
    FlyingPlayer (long timestamp, float speed) {
        this.flyStartTimestamp = timestamp;
        this.previousFlyingSpeed = speed;
    }
}
