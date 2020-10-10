package city.newnan.newnanplus.fly;

public class FlyingPlayer {
    public long flyStartTimestamp;
    public float previousFlyingSpeed;
    FlyingPlayer (long timestamp, float speed) {
        this.flyStartTimestamp = timestamp;
        this.previousFlyingSpeed = speed;
    }
}
