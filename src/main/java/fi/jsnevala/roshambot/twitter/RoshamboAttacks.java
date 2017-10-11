package fi.jsnevala.roshambot.twitter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum RoshamboAttacks {
    ROCK, PAPER, SCISSORS;

    public static List<String> asList() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.toList());
    }
}
