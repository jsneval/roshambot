package fi.jsnevala.roshambot.twitter;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DefaultRoshamboAttackCountService implements RoshamboAttackCountService {

    private static final long EXPIRE_TIME_MS = TimeUnit.HOURS.toMillis(12);
    private static final int MAX_ATTACK_COUNT = 10;
    private Map<Long, Integer> attackCounts;
    private long lastClearMillis = 0;

    public DefaultRoshamboAttackCountService() {
        attackCounts = new HashMap<>();
    }

    @Override
    public void clearIfExpired() {
        long now = System.currentTimeMillis();
        if ((now - lastClearMillis) > EXPIRE_TIME_MS) {
            attackCounts.clear();
            lastClearMillis = now;
        }
    }

    @Override
    public void storeAttack(long userId) {
        int currentAttackCount = 0;
        if (attackCounts.containsKey(userId) && (attackCounts.get(userId) != null)) {
            currentAttackCount = attackCounts.get(userId);
        }
        attackCounts.put(userId, currentAttackCount + 1);
    }

    @Override
    public boolean isAttackAllowed(long userId) {
        return !attackCounts.containsKey(userId) || (attackCounts.get(userId) == null) || (attackCounts.get(userId) < MAX_ATTACK_COUNT);
    }

    @Override
    public boolean isLimitReachedNotificationSent(long userId) {
        return attackCounts.containsKey(userId) && (attackCounts.get(userId) != null) && (attackCounts.get(userId) > MAX_ATTACK_COUNT);
    }
}
