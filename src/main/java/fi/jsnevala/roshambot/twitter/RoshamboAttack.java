package fi.jsnevala.roshambot.twitter;

import java.util.HashMap;
import java.util.Map;

public enum RoshamboAttack {

    ROCK(0, -1, 1), PAPER(1, 0, -1), SCISSORS(-1, 1, 0);

    private Map<String, Integer> results = new HashMap<>();

    RoshamboAttack(int resultAgainstRock, int resultAgainstPaper, int resultAgainstScissors) {
        results.put("ROCK", resultAgainstRock);
        results.put("PAPER", resultAgainstPaper);
        results.put("SCISSORS", resultAgainstScissors);
    }

    public int getResultAgainst(RoshamboAttack attack) {
        return results.get(RoshamboAttack.valueOf(attack.name()).name());
    }

}
