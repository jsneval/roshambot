package fi.jsnevala.roshambot.twitter;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class DefaultRoshamboAttackService implements RoshamboAttackService {
    @Override
    public String getAttack() {
        int index = ThreadLocalRandom.current().nextInt(0, RoshamboAttacks.values().length);
        return RoshamboAttacks.values()[index].name();
    }
}
