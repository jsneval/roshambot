package fi.jsnevala.roshambot.twitter;

import java.util.concurrent.TimeUnit;

class AppConstants {
    private static final int TWITTER_RATE_LIMIT = 75;
    private static final int TWITTER_RATE_LIMIT_WINDOW_MINUTES = 15;

    private static final long TWITTER_RATE_LIMIT_WINDOW_MS = TimeUnit.MILLISECONDS.convert(TWITTER_RATE_LIMIT_WINDOW_MINUTES, TimeUnit.MINUTES);
    static final long SLEEP_INTERVAL_MS = TWITTER_RATE_LIMIT_WINDOW_MS / (TWITTER_RATE_LIMIT - 10);

    static final boolean DEBUG = true;

}
