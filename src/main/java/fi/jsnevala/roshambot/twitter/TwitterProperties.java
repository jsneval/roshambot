package fi.jsnevala.roshambot.twitter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
class TwitterProperties {

    @Value("${twitter.consumer.key}")
    private String consumerKey;

    @Value("${twitter.consumer.secret}")
    private String consumerSecret;

    @Value("${twitter.access.token}")
    private String accessToken;

    @Value("${twitter.access.token.secret}")
    private String accessTokenSecret;

    String getConsumerKey() {
        return consumerKey;
    }

    String getConsumerSecret() {
        return consumerSecret;
    }

    String getAccessToken() {
        return accessToken;
    }

    String getAccessTokenSecret() {
        return accessTokenSecret;
    }
}
