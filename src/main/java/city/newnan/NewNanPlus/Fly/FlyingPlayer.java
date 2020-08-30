package city.newnan.NewNanPlus.Fly;

public class FlyingPlayer {
    public long FlyStartTimestamp;
    public float PreviousFlyingSpeed;
    FlyingPlayer (long timestamp, float speed) {
        this.FlyStartTimestamp = timestamp;
        this.PreviousFlyingSpeed = speed;
    }
}
