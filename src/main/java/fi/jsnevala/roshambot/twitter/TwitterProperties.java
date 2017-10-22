package fi.jsnevala.roshambot.twitter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
class TwitterProperties {

    @Value("${TWITTER_CONSUMER_KEY}")
    private String consumerKey;

    @Value("${TWITTER_CONSUMER_SECRET}")
    private String consumerSecret;

    @Value("${TWITTER_ACCESS_TOKEN}")
    private String accessToken;

    @Value("${TWITTER_ACCESS_TOKEN_SECRET}")
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
