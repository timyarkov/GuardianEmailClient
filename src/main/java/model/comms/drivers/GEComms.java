package model.comms.drivers;

import model.comms.exceptions.GECommsException;
import model.comms.payloads.*;
import model.items.GContent;
import model.items.GTag;

import java.util.List;

/**
 * General system for communicating with the APIs.
 */
public interface GEComms {
    // The Guardian Data
    /**
     * Requests tags based on the payload.
     * @param payload Payload to pass to request.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public GEResponse getTags(GTagPayload payload) throws GECommsException;

    /**
     * Requests content based on the payload.
     * @param payload Payload to pass to request.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public GEResponse getContent(GContentPayload payload) throws GECommsException;

    // Email Data
    /**
     * Makes an email send request.
     * @param payload Data to make request with.
     * @return Response data; if successful, should be empty.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public GEResponse sendEmail(ESendPayload payload) throws GECommsException;

    // Reddit
    /**
     * Gets a reddit access token.
     * @param payload Access data.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public GEResponse getRedditToken(RTokenPayload payload) throws GECommsException;

    /**
     * Sends a request to post to reddit.
     * @param payload Data to make request with.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public GEResponse postReddit(RPostPayload payload) throws GECommsException;

    // Utilities
    /**
     * Makes the string for the output API to send/post.
     * @param tag Tag to send/post.
     * @param content Content to send/post.
     * @param encode Whether to encode (i.e. space -> %20, & -> %26)
     * @return String of output to send/post.
     */
    public default String makeOutputContentBody(GTag tag, List<GContent> content, boolean encode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here are some articles for the tag ")
          .append(tag.id())
          .append(":\n");

        for (GContent gc : content) {
            // Account for potential ampersands
            String webTitle = gc.webTitle();

            if (encode) {
                webTitle = webTitle.replaceAll("&", "%26")
                                   .replaceAll(";", "%3b");
            }

            sb.append("- ")
              .append(webTitle).append(" | ")
              .append("Published ")
              .append(gc.webPublicationDate()).append("\n");
        }

        return sb.toString();
    }
}
