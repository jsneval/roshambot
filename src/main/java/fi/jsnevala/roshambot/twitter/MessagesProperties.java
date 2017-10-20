package fi.jsnevala.roshambot.twitter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Arrays;
import java.util.List;

@Configuration
@PropertySource("classpath:messages.properties")
public class MessagesProperties {

    @Value("${messages.win}")
    private String winMessages;

    @Value("${messages.lose}")
    private String loseMessages;

    @Value("${messages.draw}")
    private String drawMessages;

    @Value("${messages.limit.reached}")
    private String limitReachedMessage;

    List<String> getWinMessages() {
        return Arrays.asList(winMessages.split(";"));
    }

    List<String> getLoseMessages() {
        return Arrays.asList(loseMessages.split(";"));
    }

    List<String> getDrawMessages() {
        return Arrays.asList(drawMessages.split(";"));
    }

    String getLimitReachedMessage() {
        return limitReachedMessage;
    }
}
