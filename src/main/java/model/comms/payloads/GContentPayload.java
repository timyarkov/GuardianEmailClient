package model.comms.payloads;

import model.items.GTag;

/**
 * Data content to send with a Guardian content request.
 * @param tag Tag for content being searched for.
 * @param query Query for content.
 * @param page Page of content to search on.
 * @param pageSize How big a page should be.
 */
public record GContentPayload(
        GTag tag,
        String query,
        int page,
        int pageSize
) { }
