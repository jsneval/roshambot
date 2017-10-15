package fi.jsnevala.roshambot.twitter;

public interface RoshamboAttackCountService {
    void clearIfExpired();
    void storeAttack(long userId);
    boolean isAttackAllowed(long userId);
    boolean isLimitReachedNotificationSent(long userId);
}
