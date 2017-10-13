package fi.jsnevala.roshambot.twitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class TwitterListenerThread extends Thread {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    //TODO refactor to twitter service etc

    private Twitter twitter;

    private static final int TWITTER_RATE_LIMIT = 75;
    private static final int TWITTER_RATE_LIMIT_WINDOW_MINUTES = 15;

    private static final long TWITTER_RATE_LIMIT_WINDOW_MILLIS = TimeUnit.MILLISECONDS.convert(TWITTER_RATE_LIMIT_WINDOW_MINUTES, TimeUnit.MINUTES);

    TwitterListenerThread(TwitterProperties properties) {
        authenticateToTwitter(properties);
        this.start();
    }

    @Override
    public void run() {
        long lastUpdateMillis = System.currentTimeMillis();
        while (true) {
            try {
                processMentions(lastUpdateMillis);
            } catch (TwitterException e) {
                log.error("Cannot get mentions.", e);
            }

            lastUpdateMillis = System.currentTimeMillis();

            sleepUntilNextUpdate();
        }
    }

    private void processMentions(long lastUpdateMillis) throws TwitterException {
        ResponseList<Status> mentions = twitter.getMentionsTimeline();
        mentions.stream().filter(mention -> isCounterAttackNeeded(mention, lastUpdateMillis)).forEach(this::sendCounterAttack);
    }

    private void sleepUntilNextUpdate() {
        // TODO modify sleep time based on rate limit remaining

        try {
            TimeUnit.MILLISECONDS.sleep(TWITTER_RATE_LIMIT_WINDOW_MILLIS / (TWITTER_RATE_LIMIT - 10));
        } catch (InterruptedException e) {
            log.info("Sleep interrupted.", e);
        }
    }

    private void sendCounterAttack(Status status) {
        int index = ThreadLocalRandom.current().nextInt(0, 3);
        String attack = RoshamboAttacks.values()[index].name();
        log.info(String.format("Got: %s, attack with: %s", status.getText(), attack));

    }

    private void authenticateToTwitter(TwitterProperties properties) {
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
    }

    private boolean isCounterAttackNeeded(Status status, long lastUpdateMillis) {
        long statusTimeMillis = status.getCreatedAt().getTime();

        try {
            if (status.getUser().getId() != this.twitter.getId()) {
                if (statusTimeMillis > lastUpdateMillis) {
                    return isAttack(status.getText());
                }
            }
        } catch (TwitterException e) {
            log.error(String.format("Cannot parse text '%s'", status.getText()), e);
        }
        return false;
    }

    private boolean isAttack(String text) {
        List<String> regexAttacks = RoshamboAttacks.asList().stream().map(attack -> String.format("\\W%s\\W", attack)).collect(Collectors.toList());
        String regex = String.join("|", regexAttacks);
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

}
