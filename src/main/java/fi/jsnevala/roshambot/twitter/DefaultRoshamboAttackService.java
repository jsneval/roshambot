package fi.jsnevala.roshambot.twitter;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class DefaultRoshamboAttackService implements RoshamboAttackService {
    @Override
    public RoshamboAttack getAttack() {
        int index = ThreadLocalRandom.current().nextInt(0, RoshamboAttack.values().length);
        return RoshamboAttack.values()[index];
    }
}
