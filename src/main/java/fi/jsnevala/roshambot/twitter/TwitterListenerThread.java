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

import java.util.Arrays;
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

    private static final boolean DEBUG = true;

    private RoshamboAttackService roshamboAttackService;
    private RoshamboAttackCountService roshamboAttackCountService;
    private MessagesProperties messagesProperties;

    TwitterListenerThread(TwitterProperties twitterProperties, MessagesProperties messagesProperties, RoshamboAttackService roshamboAttackService, RoshamboAttackCountService roshamboAttackCountService) {
        this.roshamboAttackService = roshamboAttackService;
        this.messagesProperties = messagesProperties;
        this.roshamboAttackCountService = roshamboAttackCountService;
        authenticateToTwitter(twitterProperties);
        this.start();
    }

    // todo reply with "try to make up your mind" if multiple attacks are found in one tweet

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

    private void sendCounterAttack(Status status) {
        roshamboAttackCountService.clearIfExpired();
        roshamboAttackCountService.storeAttack(status.getUser().getId());
        if (roshamboAttackCountService.isAttackAllowed(status.getUser().getId())) {
            RoshamboAttack counterAttack = roshamboAttackService.getAttack();
            String attackFromStatus = getAttackFromStatus(status.getText());
            if (attackFromStatus != null) {
                String replyText = getReplyText(RoshamboAttack.valueOf(attackFromStatus.toUpperCase()), counterAttack);
                tweetReply(status, replyText);
            }
        } else if (!roshamboAttackCountService.isLimitReachedNotificationSent(status.getUser().getId())) {
            tweetReply(status, messagesProperties.getLimitReachedMessage());
        }
    }

    private void tweetReply(Status status, String replyText) {
        StatusUpdate reply = new StatusUpdate(replyText);
        log.info(String.format("Received %s, reply with: %s", status.getText(), replyText));

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

        if (!DEBUG) {
            t.start();
        }
    }

    private String getReplyText(RoshamboAttack attackFromStatus, RoshamboAttack counterAttack) {
        String replyText = "";
        int result = counterAttack.getResultAgainst(attackFromStatus);
        if (result == 0) {
            int index = ThreadLocalRandom.current().nextInt(0, messagesProperties.getDrawMessages().size());
            replyText = messagesProperties.getDrawMessages().get(index);
        } else if (result < 0) {
            int index = ThreadLocalRandom.current().nextInt(0, messagesProperties.getLoseMessages().size());
            replyText = messagesProperties.getLoseMessages().get(index);
        } else {
            int index = ThreadLocalRandom.current().nextInt(0, messagesProperties.getWinMessages().size());
            replyText = messagesProperties.getWinMessages().get(index);
        }
        return String.format("%s! %s", counterAttack, replyText);
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

        try {
            if (DEBUG || (status.getUser().getId() != this.twitter.getId())) {
                if (statusTimeMillis > lastUpdateMillis) {
                    String attack = getAttackFromStatus(status.getText());
                    return attack != null;
                }
            }
        } catch (TwitterException e) {
            log.error(String.format("Cannot parse text '%s'", status.getText()), e);
        }
        return false;
    }

    private String getAttackFromStatus(String text) {
        List<String> regexAttacks = Arrays.stream(RoshamboAttack.values()).map(attack -> String.format("(^|\\W)(%s)($|\\W)", attack))
            .collect(Collectors.toList());
        String regex = String.format(".*(%s).*", String.join("|", regexAttacks));
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private void sleepUntilNextUpdate() {
        // TODO modify sleep time based on rate limit remaining

        try {
            TimeUnit.MILLISECONDS.sleep(SLEEP_INTERVAL_MS);
        } catch (InterruptedException e) {
            log.info("Sleep interrupted.", e);
        }
    }

}
