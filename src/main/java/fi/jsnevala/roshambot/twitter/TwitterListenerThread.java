package fi.jsnevala.roshambot.twitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TwitterListenerThread extends Thread {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    //TODO refactor to twitter service etc

    private Twitter twitter;

    private static final String TWITTER_RESOURCE_MENTIONS_TIMELINE = "/statuses/mentions_timeline";
    private static final int TWITTER_RATE_LIMIT = 75;
    private static final int TWITTER_RATE_LIMIT_WINDOW_MINUTES = 15;

    private static final long TWITTER_RATE_LIMIT_WINDOW_MILLIS = TimeUnit.MILLISECONDS.convert(TWITTER_RATE_LIMIT_WINDOW_MINUTES, TimeUnit.MINUTES);

    TwitterListenerThread(TwitterProperties properties) {
        twitter = TwitterFactory.getSingleton();
        try {
            twitter.setOAuthConsumer(properties.getConsumerKey(), properties.getConsumerSecret());
            AccessToken accessToken = new AccessToken(properties.getAccessToken(), properties.getAccessTokenSecret());
            twitter.setOAuthAccessToken(accessToken);
            User user = twitter.verifyCredentials();
            log.info(String.format("%s : %s", user.getId(), user.getScreenName()));
        } catch (TwitterException e) {
            if (e.getStatusCode() == 401) {
                log.error("Unable to get the access token.", e);
            }
        }

        this.start();
    }

    @Override
    public void run() {
        long lastUpdateMillis = System.currentTimeMillis();
        while (true) {
            try {
                // TODO modify sleep time based on rate limit remaining

                Map<String, RateLimitStatus> rateLimits = twitter.getRateLimitStatus();
                int rateLimitRemaining = -1;
                if (rateLimits.containsKey(TWITTER_RESOURCE_MENTIONS_TIMELINE)) {
                    rateLimitRemaining = rateLimits.get(TWITTER_RESOURCE_MENTIONS_TIMELINE).getRemaining();
                } else {
                    log.error(String.format("Cannot get rate limit status for '%s'", TWITTER_RESOURCE_MENTIONS_TIMELINE));
                }
                ResponseList<Status> mentions = twitter.getMentionsTimeline();
                final long finalLastUpdateMillis = lastUpdateMillis;
                mentions.forEach(mention -> parseText(mention, finalLastUpdateMillis));
                log.debug(String.format("Mentions: %s, rate limit remaining: %s", mentions.size(), rateLimitRemaining));
            } catch (TwitterException e) {
                log.error("Cannot get mentions.", e);
            }
            lastUpdateMillis = System.currentTimeMillis();
            try {
                TimeUnit.MILLISECONDS.sleep(TWITTER_RATE_LIMIT_WINDOW_MILLIS / (TWITTER_RATE_LIMIT - 10));
            } catch (InterruptedException e) {
                log.info("Sleep interrupted.", e);
            }

        }


    }

    private void parseText(Status status, long lastUpdateMillis) {
        long statusTimeMillis = status.getCreatedAt().getTime();
        if (statusTimeMillis > lastUpdateMillis) {
            if (isAttack(status.getText())) {
                int index = ThreadLocalRandom.current().nextInt(0, 3);
                String attack = RoshamboAttacks.values()[index].name();
                log.info(String.format("Got: %s, attack with: %s", status.getText(), attack));
            }
        }
    }

    private boolean isAttack(String text) {
        String regex = String.join("|", RoshamboAttacks.asList());
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

}
