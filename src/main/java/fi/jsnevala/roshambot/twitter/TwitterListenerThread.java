package fi.jsnevala.roshambot.twitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
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

    private Twitter twitter;

    private static final int TWITTER_RATE_LIMIT = 75;
    private static final int TWITTER_RATE_LIMIT_WINDOW_MINUTES = 15;

    private static final long TWITTER_RATE_LIMIT_WINDOW_MS = TimeUnit.MILLISECONDS.convert(TWITTER_RATE_LIMIT_WINDOW_MINUTES, TimeUnit.MINUTES);
    private static final long SLEEP_INTERVAL_MS = TWITTER_RATE_LIMIT_WINDOW_MS / (TWITTER_RATE_LIMIT - 10);

    private RoshamboAttackService roshamboAttackService;

    TwitterListenerThread(TwitterProperties properties, RoshamboAttackService roshamboAttackService) {
        this.roshamboAttackService = roshamboAttackService;
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
            TimeUnit.MILLISECONDS.sleep(SLEEP_INTERVAL_MS);
        } catch (InterruptedException e) {
            log.info("Sleep interrupted.", e);
        }
    }

    private void sendCounterAttack(Status status) {
        String counterAttack = roshamboAttackService.getAttack();
        log.info(String.format("Received %s, attack with: %s", status.getText(), counterAttack));
        StatusUpdate reply = new StatusUpdate(counterAttack);

        Thread t = new Thread(() -> {
            long randomWithinIntervalMillis = ThreadLocalRandom.current().nextLong(0, SLEEP_INTERVAL_MS);
            try {
                TimeUnit.MILLISECONDS.sleep(randomWithinIntervalMillis);
            } catch (InterruptedException e) {
                log.info("Interrupted sleep.", e);
            }
            reply.setInReplyToStatusId(status.getId());
            try {
                twitter.updateStatus(reply);
            } catch (TwitterException e) {
                log.error(String.format("Cannot reply to '%s'", status.getText()), e);
            }
        });

        t.start();
    }

    private void authenticateToTwitter(TwitterProperties properties) {
        twitter = TwitterFactory.getSingleton();
        try {
            twitter.setOAuthConsumer(properties.getConsumerKey(), properties.getConsumerSecret());
            AccessToken accessToken = new AccessToken(properties.getAccessToken(), properties.getAccessTokenSecret());
            twitter.setOAuthAccessToken(accessToken);
            User user = twitter.verifyCredentials();
            log.info(String.format("Authenticated to Twitter as %s (%s)", user.getScreenName(), user.getId()));
        } catch (TwitterException e) {
            if (e.getStatusCode() == 401) {
                log.error("Unable to get the access token.", e);
            }
        }
    }

    private boolean isCounterAttackNeeded(Status status, long lastUpdateMillis) {
        long statusTimeMillis = status.getCreatedAt().getTime();

//        try {
//            if (status.getUser().getId() != this.twitter.getId()) {
                if (statusTimeMillis > lastUpdateMillis) {
                    return isAttack(status.getText());
                }
//            }
//        } catch (TwitterException e) {
//            log.error(String.format("Cannot parse text '%s'", status.getText()), e);
//        }
        return false;
    }

    private boolean isAttack(String text) {
        List<String> regexAttacks = RoshamboAttacks.asList().stream().map(attack -> String.format("(^|\\W)%s($|\\W)", attack)).collect(Collectors.toList());
        String regex = String.join("|", regexAttacks);
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

}
