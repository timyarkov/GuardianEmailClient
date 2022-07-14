package model.comms.drivers;

import com.google.gson.*;
import model.comms.exceptions.GECommsException;
import model.comms.payloads.*;
import model.comms.util.RedditAuthenticator;
import model.env.Environment;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Online-enabled Guardian-Email communicator.
 */
public class GEOnlineComms implements GEComms {
    private static final String GUARDIAN_URL = "https://content.guardianapis.com";
    private static final String SENDGRID_URL = "https://api.sendgrid.com";
    private static final String REDDIT_URL = "https://www.reddit.com";
    private static final String REDDIT_OAUTH_URL = "https://oauth.reddit.com";
    private static final String REDDIT_USER_AGENT = "GEClient/0.1";
    private static Environment env = new Environment();

    // Utilities
    /**
     * Makes an HTTP POST request to The Guardian's API (i.e. formatting auth and token
     * for use with it).
     * @param endpoint Endpoint to send it to; NOT full URL (i.e. just /content, /tags, etc.).
     * @param body Body content to send in the POST request; key value pairs of parameter:value
     * @throws GECommsException In any case something goes wrong
     *                          (client error, server error, or other)
     * @return Response.
     */
    public static GEResponse gHttpGetRequest(String endpoint,
                                                       Map<String, String> body) throws GECommsException {
        try {
            // Setup URL with query string for parameters
            StringBuilder sb = new StringBuilder(GUARDIAN_URL + endpoint);
            sb.append("?api-key=").append(env.getenv("INPUT_API_KEY"));

            for (Map.Entry<String, String> param : body.entrySet()) {
                sb.append("&%s=".formatted(param.getKey())).append(param.getValue());
            }

            // Do the request
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest req = HttpRequest.newBuilder(new URI(sb.toString()))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            return new GEResponse(res.body(), res.statusCode());
        } catch (IOException | InterruptedException | IllegalStateException e) {
            throw new GECommsException(-1,
                    "IOException/InterruptedException/IllegalStateException thrown; " +
                    e.getMessage());
        } catch (URISyntaxException e) {
            throw new GECommsException(-1,
                    "URISyntaxException thrown; " + e.getMessage());
        }
    }

    /**
     * Makes an HTTP POST request to SendGrid's email API (i.e. formatting auth and token
     * for use with it).
     * @param endpoint Endpoint to send it to; NOT full URL (i.e. just /content, /tags, etc.).
     * @param body Body content to send in the POST request; expected JSON format
     * @throws GECommsException In any case something goes wrong
     *                          (client error, server error, or other)
     * @return Response.
     */
    public static GEResponse eHttpPostRequest(String endpoint,
                                                        String body) throws GECommsException {
        try {
            HttpRequest req = HttpRequest.newBuilder(new URI(SENDGRID_URL + endpoint))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .setHeader("Authorization", "Bearer " +
                                env.getenv("SENDGRID_API_KEY"))
                    .setHeader("Content-Type", "application/json")
                    .build();

            // Do the request
            HttpClient client = HttpClient.newBuilder().build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            return new GEResponse(res.body(), res.statusCode());
        } catch (IOException | InterruptedException | IllegalStateException e) {
            throw new GECommsException(-1,
                    "IOException/InterruptedException/IllegalStateException thrown; " +
                    e.getMessage());
        } catch (URISyntaxException e) {
            throw new GECommsException(-1,
                    "URISyntaxException thrown; " + e.getMessage());
        }
    }

    /**
     * Makes a request for an auth token from reddit.
     * @param username Username to authenticate with. Must be registered with API keys used.
     * @param password Password to authenticate with. Must be registered with API keys used.
     * @throws GECommsException In any case something goes wrong
     *                          (client error, server error, or other)
     * @return Response from request.
     */
    public static GEResponse rGetAuthToken(String username,
                                                     String password) throws GECommsException {
        try {
            HttpRequest req = HttpRequest.newBuilder(new URI(REDDIT_URL + "/api/v1/access_token"))
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=password" +
                                                              "&username=" + username +
                                                              "&password=" + password))
                    .setHeader("user-agent", REDDIT_USER_AGENT)
                    .build();

            // Do the request
            HttpClient client = HttpClient.newBuilder()
                    .authenticator(new RedditAuthenticator())
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            return new GEResponse(res.body(), res.statusCode());
        } catch (IOException | InterruptedException | IllegalStateException e) {
            throw new GECommsException(-1,
                    "IOException/InterruptedException/IllegalStateException thrown; " +
                    e.getMessage());
        } catch (URISyntaxException e) {
            throw new GECommsException(-1,
                    "URISyntaxException thrown; " + e.getMessage());
        }
    }

    /**
     * Makes a POST type request to Reddit (OAuth API).
     * @param endpoint Endpoint to make request to.
     * @param data Data to send with request.
     * @param token OAuth token to pass in.
     * @return Response.
     * @throws GECommsException If something goes wrong.
     */
    public static GEResponse rHttpPostRequest(String endpoint,
                                                        String data,
                                                        String token) throws GECommsException {
        try {
            HttpRequest req = HttpRequest.newBuilder(new URI(REDDIT_OAUTH_URL + endpoint))
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .setHeader("Authorization", "bearer " + token)
                    .setHeader("User-Agent", REDDIT_USER_AGENT)
                    .build();

            // Do the request
            HttpClient client = HttpClient.newBuilder().build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            return new GEResponse(res.body(), res.statusCode());
        } catch (IOException | InterruptedException | IllegalStateException e) {
            throw new GECommsException(-1,
                    "IOException/InterruptedException/IllegalStateException thrown; " +
                    e.getMessage());
        } catch (URISyntaxException e) {
            throw new GECommsException(-1,
                    "URISyntaxException thrown; " + e.getMessage());
        }
    }

    // The Guardian Data
    /**
     * Requests tags based on the payload.
     * @param payload Payload to pass to request.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public GEResponse getTags(GTagPayload payload) throws GECommsException {
        Map<String, String> body = Map.ofEntries(
                Map.entry("q", URLEncoder.encode(payload.query(),
                                                    StandardCharsets.UTF_8)),
                Map.entry("page", URLEncoder.encode(String.valueOf(payload.page()),
                                                    StandardCharsets.UTF_8)),
                Map.entry("page-size", URLEncoder.encode(String.valueOf(payload.pageSize()),
                                                    StandardCharsets.UTF_8)),
                Map.entry("format", "json")
        );

        return gHttpGetRequest("/tags", body);
    }

    /**
     * Requests content based on the payload.
     * @param payload Payload to pass to request.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public GEResponse getContent(GContentPayload payload) throws GECommsException {
        Map<String, String> body = Map.ofEntries(
                Map.entry("q", URLEncoder.encode(payload.query(),
                                                    StandardCharsets.UTF_8)),
                Map.entry("tag", URLEncoder.encode(payload.tag().id(),
                                                    StandardCharsets.UTF_8)),
                Map.entry("page", URLEncoder.encode(String.valueOf(payload.page()),
                                                    StandardCharsets.UTF_8)),
                Map.entry("page-size", URLEncoder.encode(String.valueOf(payload.pageSize()),
                                                    StandardCharsets.UTF_8)),
                Map.entry("format", "json")
        );

        return gHttpGetRequest("/search", body);
    }

    // Email Data
    /**
     * Makes an email send request.
     * @param payload Data to make request with.
     * @return Response data; if successful, should be empty.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public GEResponse sendEmail(ESendPayload payload) throws GECommsException {
        // Construct JSON to send; format of:
        /*
        {
            "personalizations":
                [
                    {
                        "to":
                            [
                                {
                                    "email":"recipient@email.com"
                                }
                            ]
                    }
                ],
            "from":
                {
                    "email":"sender@email.com"
                },
            "subject":"email subject",
            "content":
                [
                    {
                        "type":"text/plain",
                        "value":"content to put in email"
                    }
                ]
        }
        */
        JsonObject data = new JsonObject();

        // Personalizations; setting up recipient
        JsonArray pArr = new JsonArray(1);
        JsonArray toArr = new JsonArray(1);

        JsonObject recipient = new JsonObject();
        recipient.add("email", new JsonPrimitive(payload.recipient()));

        toArr.add(recipient);

        JsonObject to = new JsonObject();
        to.add("to", toArr);

        pArr.add(to);

        data.add("personalizations", pArr);

        // From
        JsonObject from = new JsonObject();
        from.add("email", new JsonPrimitive(env.getenv("SENDGRID_API_EMAIL")));

        data.add("from", from);

        // Subject
        data.add("subject",
                new JsonPrimitive("GE Client: Articles for tag " + payload.tag().id()));

        // Content
        String contentStr = makeOutputContentBody(payload.tag(), payload.content(), false);

        JsonArray cArr = new JsonArray(1);

        JsonObject content = new JsonObject();
        content.add("type", new JsonPrimitive("text/plain"));
        content.add("value", new JsonPrimitive(contentStr));

        cArr.add(content);

        data.add("content", cArr);

        return eHttpPostRequest("/v3/mail/send", data.toString());
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
        return rGetAuthToken(payload.username(), payload.password());
    }

    /**
     * Sends a request to post to reddit.
     * @param payload Data to make request with.
     * @return Response data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public GEResponse postReddit(RPostPayload payload) throws GECommsException {
        // Construct data
        String sb = "title=GE Client: Articles for tag " + payload.tag() +
                    "&sr=u_" + payload.username() +
                    "&text=" + makeOutputContentBody(payload.tag(), payload.content(), true) +
                    "&kind=self";

        return rHttpPostRequest("/api/submit", sb, payload.token());
    }
}
