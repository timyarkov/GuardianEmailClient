package model.comms.payloads;

import model.items.GContent;
import model.items.GTag;

import java.util.List;

/**
 * Data to send with a Reddit post request.
 * @param username Username of posting account.
 * @param token Token to authenticate with. If null, means no token.
 * @param tag Tag associated with the content.
 * @param content List of content to post.
 */
public record RPostPayload (
        String username,
        String token,
        GTag tag,
        List<GContent> content
){ }
