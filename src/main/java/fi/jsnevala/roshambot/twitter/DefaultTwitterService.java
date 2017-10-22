package fi.jsnevala.roshambot.twitter;

import org.springframework.stereotype.Service;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;

@Service
public class DefaultTwitterService implements TwitterService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private Twitter twitter;

    public DefaultTwitterService(TwitterProperties twitterProperties) {
        authenticateToTwitter(twitterProperties);
    }

    @Override
    public void tweetReply(Status status, String replyText) {
        StatusUpdate reply = new StatusUpdate(replyText);
        log.info(String.format("Received %s, reply with: %s", status.getText(), replyText));

        Thread t = new Thread(() -> {
            long randomWithinIntervalMillis = ThreadLocalRandom.current().nextLong(0, AppConstants.SLEEP_INTERVAL_MS);
            try {
                TimeUnit.MILLISECONDS.sleep(randomWithinIntervalMillis);
            } catch (InterruptedException e) {
                log.info("Interrupted sleep.", e);
            }
            reply.setInReplyToStatusId(status.getId());
            try {
                twitter.updateStatus(reply);
                log.info(String.format("Sent reply to '%s' (%s)", status.getText(), reply.getInReplyToStatusId()));
            } catch (TwitterException e) {
                log.error(String.format("Cannot reply to '%s'", status.getText()), e);
            }
        });

        if (!AppConstants.DEBUG) {
            t.start();
        }
    }

    @Override
    public long getUserId() throws TwitterException {
        return this.twitter.getId();
    }

    @Override
    public ResponseList<Status> getMentionsTimeline() throws TwitterException {
        return twitter.getMentionsTimeline();
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



}
