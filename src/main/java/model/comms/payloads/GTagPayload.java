package model.comms.payloads;

/**
 * Data content to send with a Guardian tag request.
 * @param query Query to search.
 * @param page Page to search tags on.
 * @param pageSize Size of returned page.
 */
public record GTagPayload(
        String query,
        int page,
        int pageSize
) { }
