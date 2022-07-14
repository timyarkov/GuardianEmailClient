package model.comms.payloads;

/**
 * Data to send with a Reddit OAuth token request.
 * @param username Username to authenticate with.
 * @param password Password to authenticate with.
 */
public record RTokenPayload(
        String username,
        String password
) { }
