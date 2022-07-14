package model.comms.util;

import model.env.Environment;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Authenticator for reddit (getting access tokens).
 * <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/net/http-auth.html">Solution from java documentation</a>
 */
public class RedditAuthenticator extends Authenticator {
    private Environment env;

    /**
     * Creates a Reddit Authenticator.
     */
    public RedditAuthenticator() {
        this.env = new Environment();
    }

    /**
     * Injects a new environment. If invalid (i.e. null),
     * the old environment will not be replaced.
     * @param env Environment to inject.
     */
    public void injectNewEnv(Environment env) {
        if (env != null) {
            this.env = env;
        }
    }

    /**
     * Gets the PasswordAuthentication object to do basic authorisation
     * for reddit HTTP requests.
     * @return PasswordAuthentication object.
     */
    public PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(this.env.getenv("REDDIT_API_CLIENT"),
                                          this.env.getenv("REDDIT_API_SECRET").toCharArray());
    }
}
