package model.items;

/**
 * A Reddit OAuth token.
 * @param token Token.
 * @param expiry When the token will expire.
 */
public record RedditToken(
        String token,
        int expiry
) { }
