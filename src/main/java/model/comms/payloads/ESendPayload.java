package model.comms.payloads;

import model.items.GContent;
import model.items.GTag;

import java.util.List;

/**
 * Data content to send with an email send request.
 * @param recipient Email address of recipient.
 * @param content List of content items to send.
 * @param tag Tag associated with content being sent.
 */
public record ESendPayload(
        String recipient,
        GTag tag,
        List<GContent> content
) { }
