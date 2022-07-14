package model.comms.manager;

import com.google.gson.JsonObject;
import model.comms.drivers.GEComms;
import model.comms.exceptions.GECommsException;
import model.comms.payloads.*;
import model.comms.util.JSONParser;
import model.db.GEDatabase;
import model.env.Environment;
import model.items.RedditToken;

/**
 * Allows for managing communications systems,
 * including cache usage and such.
 */
public interface GECommsManager {
    // Module Injection/System State
    /**
     * Injects a database to use for caching. If null,
     * caching will not be used.
     * @param gedb Database to inject for use. Pass in
     *             null for no database usage.
     */
    public void injectDatabase(GEDatabase gedb);

    /**
     * Injects a new JSON parser. If invalid (i.e. null),
     * the old parser is not replaced.
     * @param parser Parser to inject.
     */
    public void injectNewParser(JSONParser parser);

    /**
     * Injects new driver classes. If one is invalid (i.e. null),
     * the respective old driver is not replaced.
     * @param online Online comms driver to inject.
     * @param offline Offline comms driver to inject.
     */
    public void injectNewDrivers(GEComms online, GEComms offline);

    /**
     * Injects a new environment. If invalid (i.e. null), the
     * old one will not be replaced.
     * @param env Environment to inject.
     */
    public void injectNewEnvironment(Environment env);

    /**
     * Sets whether to use online or offline mode each
     * API. True means online, false means use the dummy version.
     * @param guardian Setting The Guardian API.
     * @param email Setting SendGrid Email API.
     * @param reddit Setting Reddit API.
     */
    public void setOnline(boolean guardian, boolean email, boolean reddit);

    // The Guardian Operations
    /**
     * Requests tags based on the payload.
     * @param payload Payload to pass to request.
     * @return JsonObject with returned data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public JsonObject getTags(GTagPayload payload) throws GECommsException;

    /**
     * If the database is set, checks whether there is cached
     * content for the given payload.
     * @param payload Payload to check caching for.
     * @return Whether there is cached content or not.
     *         Always returns false if no DB set, or if offline
     *         comms are in use.
     */
    public boolean isContentCached(GContentPayload payload);

    /**
     * If the database is set, requests that it clears its cache.
     * @return Whether clear operation was successful or not.
     *         Always returns true if no DB set, or offline comms
     *         are in use.
     */
    public boolean clearContentCache();

    /**
     * Requests content based on the payload.
     * @param payload Payload to pass to request.
     * @param useCache Whether to use cached results, if available.
     * @return JsonObject with returned data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public JsonObject getContent(GContentPayload payload,
                                 boolean useCache) throws GECommsException;

    // Email Operations
    /**
     * Makes an email send request.
     * @param payload Data to make request with.
     * @return If send was successful or not.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public boolean sendEmail(ESendPayload payload) throws GECommsException;

    // Reddit Operations
    /**
     * Attempts to get a Reddit access token; if successful, stores it internally for
     * future use.
     * @param tokenPayload Information for getting a token.
     * @return If successful, returns token duration (seconds), if failed
     *         (and no GECommsException is thrown) returns -1.
     * @throws GECommsException If something goes wrong.
     */
    public RedditToken getRedditToken(RTokenPayload tokenPayload) throws GECommsException;

    /**
     * Sends a request to post to reddit.
     * @param postPayload Data to make post request with.
     * @return If post was successful or not.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    public boolean postReddit(RPostPayload postPayload) throws GECommsException;
}
