package fi.jsnevala.roshambot.twitter;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

public interface TwitterService {
    void tweetReply(Status status, String replyText);
    long getUserId() throws TwitterException;
    ResponseList<Status> getMentionsTimeline() throws TwitterException;
}
