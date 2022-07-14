package model.comms.drivers;

import model.comms.exceptions.GECommsException;
import model.comms.payloads.*;

/**
 * Acts as a mock communicator, returning the same data every time.
 */
public class GEDummyComms implements GEComms {
    private long delay; // For testing concurrency, etc.

    /**
     * Creates a dummy communicator.
     * @param delay Delay to add to "communications" (milliseconds).
     */
    public GEDummyComms(long delay) {
        this.delay = delay;
    }
    
    // The Guardian Data
    /**
     * Requests tags based on the payload.
     * @param body Payload to pass to request.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public GEResponse getTags(GTagPayload body) {
        // Simulate delay
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Everything fine just skip sleep then
        }

        String json =
                """
                {
                    "response": {
                        "status": "ok",
                        "userTier": "free",
                        "total": 65,
                        "startIndex": 1,
                        "pageSize": 10,
                        "currentPage": 1,
                        "pages": 7,
                        "results": [
                            {
                                "id": "katine/football",
                                "type": "keyword",
                                "webTitle": "Football",
                                "webUrl": "http://www.theguardian.com/katine/football",
                                "apiUrl": "http://beta.content.guardianapis.com/katine/football",
                                "sectionId": "katine",
                                "sectionName": "Katine"
                            }
                        ]
                    }
                }
                """;

        return new GEResponse(json, 200);
    }

    /**
     * Requests content based on the payload.
     * @param body Payload to pass to request.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public GEResponse getContent(GContentPayload body) {
        // Simulate delay
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Everything fine just skip sleep then
        }

        String json =
                """
                {
                    "response": {
                        "status": "ok",
                        "userTier": "free",
                        "total": 1,
                        "startIndex": 1,
                        "pageSize": 10,
                        "currentPage": 1,
                        "pages": 1,
                        "orderBy": "newest",
                        "results": [
                            {
                                "id": "politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                                "sectionId": "politics",
                                "sectionName": "Politics",
                                "webPublicationDate": "2014-02-17T12:05:47Z",
                                "webTitle": "Alex Salmond speech – first minister hits back over Scottish independence – live",
                                "webUrl": "https://www.theguardian.com/politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                                "apiUrl": "https://content.guardianapis.com/politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live"
                            },
                            {
                                "id": "politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                                "sectionId": "politics",
                                "sectionName": "Politics",
                                "webPublicationDate": "2014-02-17T12:05:47Z",
                                "webTitle": "Pingu becomes President of the Antarctic; what happens next?",
                                "webUrl": "https://www.youtube.com/watch?v=aYNXqKaZWR4",
                                "apiUrl": "https://content.guardianapis.com/politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live"
                            }
                        ]
                    }
                }
                """;

        return new GEResponse(json, 200);
    }

    // Email Data
    /**
     * Makes an email send request.
     * @param payload Data to make request with.
     * @return Response data; if successful, should be empty.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public GEResponse sendEmail(ESendPayload payload) {
        // Simulate delay
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Everything fine just skip sleep then
        }

        // Print to terminal what would be output
        System.out.println(makeOutputContentBody(payload.tag(), payload.content(), false));

        // Simulate how SendGrid email works; response body is empty if all ok
        return new GEResponse("", 200);
    }

    // Reddit
    /**
     * Gets a reddit access token.
     * @param payload Access data.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public GEResponse getRedditToken(RTokenPayload payload) throws GECommsException {
        return new GEResponse("{\"access_token\": \"pingu's key\", " +
                                     "\"token_type\": \"bearer\", " +
                                     "\"expires_in\": 86400, " +
                                     "\"scope\": \"*\"}", 200);
    }

    /**
     * Sends a request to post to reddit.
     * @param payload Data to make request with.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public GEResponse postReddit(RPostPayload payload) throws GECommsException {
        // Simulate delay
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Everything fine just skip sleep then
        }

        // Print to terminal what would be output
        System.out.println(makeOutputContentBody(payload.tag(), payload.content(), false));

        // Just do empty return
        return new GEResponse("", 200);
    }
}
