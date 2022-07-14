package model.comms.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import model.comms.drivers.GEComms;
import model.comms.drivers.GEDummyComms;
import model.comms.drivers.GEOnlineComms;
import model.comms.drivers.GEResponse;
import model.comms.exceptions.GECommsException;
import model.comms.payloads.*;
import model.comms.util.JSONParser;
import model.comms.util.JSONParserImpl;
import model.db.GEDatabase;
import model.env.Environment;
import model.items.RedditToken;

/**
 * Communications management implementation.
 * @see model.comms.manager.GECommsManager
 */
public class GECommsManagerImpl implements GECommsManager {
    private boolean gOnline;
    private boolean eOnline;
    private boolean rOnline;
    private GEComms onlineComms;
    private GEComms offlineComms;
    private GEDatabase db;
    private JSONParser parser;
    private Environment env;

    /**
     * Constructs a new communications manager.
     * Database connection is by default set to null; for
     * caching, inject a database.
     * Automatically makes a JSON parser.
     * @param gOnline Whether to use the Guardian API online.
     * @param eOnline Whether to use SendGrid API online.
     * @param rOnline Whether to use Reddit API online.
     */
    public GECommsManagerImpl(boolean gOnline, boolean eOnline, boolean rOnline) {
        this.gOnline = gOnline;
        this.eOnline = eOnline;
        this.rOnline = rOnline;
        this.onlineComms = new GEOnlineComms();
        this.offlineComms = new GEDummyComms(0); // Change here if want to simulate slower comms
        this.db = null;
        this.parser = new JSONParserImpl();
        this.env = new Environment();
    }

    // Module Injection/System State
    /**
     * Injects a database to use for caching. If null,
     * caching will not be used.
     * @param gedb Database to inject for use. Pass in
     *             null for no database usage.
     */
    @Override
    public void injectDatabase(GEDatabase gedb) {
        this.db = gedb;
    }

    /**
     * Injects a new JSON parser. If invalid (i.e. null),
     * the old parser is not replaced.
     *
     * @param parser Parser to inject.
     */
    @Override
    public void injectNewParser(JSONParser parser) {
        if (parser != null) {
            this.parser = parser;
        }
    }

    /**
     * Injects new driver classes. If one is invalid (i.e. null),
     * the respective old driver is not replaced.
     * @param online Online comms driver to inject.
     * @param offline Offline comms driver to inject.
     */
    public void injectNewDrivers(GEComms online, GEComms offline) {
        if (online != null) {
            this.onlineComms = online;
        }

        if (offline != null) {
            this.offlineComms = offline;
        }
    }

    /**
     * Injects a new environment. If invalid (i.e. null), the
     * old one will not be replaced.
     * @param env Environment to inject.
     */
    @Override
    public void injectNewEnvironment(Environment env) {
        if (env != null) {
            this.env = env;
        }
    }

    /**
     * Sets whether to use online or offline mode each
     * API. True means online, false means use the dummy version.
     * @param guardian Setting The Guardian API.
     * @param email Setting SendGrid Email API.
     * @param reddit Setting Reddit API.
     */
    public void setOnline(boolean guardian, boolean email, boolean reddit){
        this.gOnline = guardian;
        this.eOnline = email;
        this.rOnline = reddit;
    }

    // The Guardian Operations
    /**
     * Requests tags based on the payload.
     * @param payload Payload to pass to request.
     * @return JsonObject with returned data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public JsonObject getTags(GTagPayload payload) throws GECommsException {
        // If online, ensure we have API key
        if (this.gOnline && this.env.getenv("INPUT_API_KEY") == null) {
            throw new GECommsException(-1,
                                       "Required environment variable INPUT_API_KEY is missing.");
        }

        // Get response
        GEResponse response;

        if (this.gOnline) {
            response = this.onlineComms.getTags(payload);
        } else {
            response = this.offlineComms.getTags(payload);
        }

        // Parse
        JsonObject ret = this.parser.parseResponse(response.body());

        // Check for errors, throw if any
        if (ret == null) {
            throw new GECommsException(-1, "Unparsable tags response: " + response.body());
        } else if (response.statusCode() >= 400 && response.statusCode() <= 599) {
            throw new GECommsException(response.statusCode(),
                    ret.get("response").getAsJsonObject().get("message").getAsString());
        }

        return ret;
    }

    /**
     * If the database is set, checks whether there is cached
     * content for the given payload.
     * @param payload Payload to check caching for.
     * @return Whether there is cached content or not.
     *         Always returns false if no DB set, or if offline
     *         comms are in use.
     */
    @Override
    public boolean isContentCached(GContentPayload payload) {
        if (!this.gOnline) {
            return false;
        }

        if (this.db == null) {
            return false;
        } else {
            String ret = this.db.getCachedContent(payload.tag(),
                                                  payload.query(),
                                                  payload.page());

            if (ret == null) {
                return false;
            } else {
                return !ret.equals("");
            }
        }
    }

    /**
     * If the database is set, requests that it clears its cache.
     * @return Whether clear operation was successful or not.
     *         Always returns true if no DB set, or offline comms
     *         are in use.
     */
    @Override
    public boolean clearContentCache() {
        if (!this.gOnline) {
            return true;
        }

        if (this.db == null) {
            return true;
        } else {
            return this.db.clearCachedContent();
        }
    }

    /**
     * Utility method for parsing and validating the response
     * for a content request.
     * @param response Response to parse and validate.
     * @return Parsed response.
     * @throws GECommsException If invalid.
     */
    private JsonObject parseValidateContent(GEResponse response) throws GECommsException {
        // Parse
        JsonObject request = this.parser.parseResponse(response.body());

        // Check for errors, throw if any
        if (request == null) {
            throw new GECommsException(-1, "Unparsable content response: " + response.body());
        } else if (response.statusCode() >= 400 && response.statusCode() <= 599) {
            throw new GECommsException(response.statusCode(),
                    request.get("response").getAsJsonObject().get("message").getAsString());
        }

        return request;
    }

    /**
     * Requests content based on the payload.
     * @param payload Payload to pass to request.
     * @param useCache Whether to use cached results, if available.
     * @return JsonObject with returned data.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public JsonObject getContent(GContentPayload payload,
                                 boolean useCache) throws GECommsException {
        // If online, ensure we have API key
        if (this.gOnline && this.env.getenv("INPUT_API_KEY") == null) {
            throw new GECommsException(-1,
                                       "Required environment variable INPUT_API_KEY is missing.");
        }

        if (this.gOnline) {
            if (useCache && this.db != null) {
                String ret = this.db.getCachedContent(payload.tag(),
                                                      payload.query(),
                                                      payload.page());

                if (ret == null) {
                    // Critical DB error!
                    throw new GECommsException(-1,
                                               "Critical DB error during content cache getting.");
                } else if (!ret.equals("")) {
                    JsonObject jsonRet = this.parser.parseResponse(ret);

                    if (jsonRet == null) {
                        throw new GECommsException(-1, "Cached content could not be parsed.");
                    } else {
                        return jsonRet;
                    }
                } else {
                    // Nothing in cache; do normal request
                    GEResponse response = this.onlineComms.getContent(payload);

                    // Parse + Validate
                    JsonObject request = this.parseValidateContent(response);

                    // Try automatically cache
                    if (this.db != null) {
                        this.db.cacheContent(payload.tag(),
                                payload.query(),
                                payload.page(),
                                request.toString());
                    }

                    return request;
                }
            } else {
                // Not using cache; do normal request
                GEResponse response = this.onlineComms.getContent(payload);

                // Parse + Validate
                JsonObject request = this.parseValidateContent(response);

                // Try automatically cache if DB present (in case want to use cache later!)
                if (this.db != null) {
                    this.db.cacheContent(payload.tag(),
                                         payload.query(),
                                         payload.page(),
                                         request.toString());
                }

                return request;
            }
        }

        // If not online, do offline
        GEResponse response = this.offlineComms.getContent(payload);

        // Parse + Validate
        return this.parseValidateContent(response);
    }

    // Email Operations
    /**
     * Makes an email send request.
     * @param payload Data to make request with.
     * @return Whether send was successful or not (if no errors should always be true)
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public boolean sendEmail(ESendPayload payload) throws GECommsException {
        // If online, ensure we have API keys
        if (this.eOnline) {
            if (this.env.getenv("SENDGRID_API_KEY") == null) {
                throw new GECommsException(-1,
                        "Required environment variable SENDGRID_API_KEY is missing.");
            } else if (this.env.getenv("SENDGRID_API_EMAIL") == null) {
                throw new GECommsException(-1,
                        "Required environment variable SENDGRID_API_EMAIL is missing.");
            }
        }

        // Get response
        GEResponse response;

        if (this.eOnline) {
            response = this.onlineComms.sendEmail(payload);
        } else {
            response = this.offlineComms.sendEmail(payload);
        }

        // If empty response, success!
        if (response.body().equals("")) {
            return true;
        }

        // Error Case; check the response
        JsonObject ret = this.parser.parseResponse(response.body());

        if (ret == null) {
            throw new GECommsException(-1, "Unparsable email send response: " + response.body());
        } else if (response.statusCode() >= 400 && response.statusCode() <= 599) {
            // Get all errors, make into one string
            JsonArray errs = ret.getAsJsonArray("errors");

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < errs.size(); i++) {
                JsonObject jo = errs.get(i).getAsJsonObject();

                if (jo.get("field") != null) {
                    if (!jo.get("field").isJsonNull()) {
                        sb.append("Problem with field ")
                                .append(jo.get("field").getAsString()).append("; ");
                    }
                }

                if (i == errs.size() - 1) {
                    sb.append(jo.get("message").getAsString());
                } else {
                    sb.append(jo.get("message").getAsString()).append(", ");
                }
            }

            throw new GECommsException(response.statusCode(), sb.toString());
        } else {
            throw new GECommsException(-1, "Unknown error in email sending");
        }
    }

    // Reddit Operations
    /**
     * Attempts to get a Reddit access token; if successful, stores it internally for
     * future use.
     * @param tokenPayload Information for getting a token.
     * @return The token gained.
     * @throws GECommsException If something goes wrong.
     */
    @Override
    public RedditToken getRedditToken(RTokenPayload tokenPayload) throws GECommsException {
        // If online, ensure we have API keys
        if (this.rOnline) {
            if (this.env.getenv("REDDIT_API_CLIENT") == null) {
                throw new GECommsException(-1,
                        "Required environment variable REDDIT_API_CLIENT is missing.");
            } else if (this.env.getenv("REDDIT_API_SECRET") == null) {
                throw new GECommsException(-1,
                        "Required environment variable REDDIT_API_SECRET is missing.");
            }
        }

        // Get token response
        GEResponse tokResponse;

        if (this.rOnline) {
            tokResponse = this.onlineComms.getRedditToken(tokenPayload);
        } else {
            tokResponse = this.offlineComms.getRedditToken(tokenPayload);
        }

        // Parse token response
        JsonObject tokRet = this.parser.parseResponse(tokResponse.body());

        // Check for errors
        if (tokRet == null) {
            throw new GECommsException(-1, "Unparsable Reddit token response: " + tokResponse.body());
        } else if (tokResponse.statusCode() >= 400 && tokResponse.statusCode() <= 599) {
            throw new GECommsException(tokResponse.statusCode(), "Error getting Reddit access token.");
        } else if (tokRet.get("error") != null) {
            if (tokRet.get("error").getAsString().equals("invalid_grant")) {
                throw new GECommsException(-1, "Invalid credentials to get a Reddit token.");
            } else {
                throw new GECommsException(-1, "Unknown Reddit token getting error.");
            }
        }

        // Check for missing fields (just in case the Reddit API decides to change how it works)
        if (tokRet.get("access_token") == null || tokRet.get("expires_in") == null) {
            throw new GECommsException(-1, "Reddit token getting does not have expected fields");
        }

        return new RedditToken(tokRet.get("access_token").getAsString(),
                               tokRet.get("expires_in").getAsInt());
    }

    /**
     * Sends a request to post to reddit.
     * @param postPayload Data to make post request with.
     * @return If post was successful or not.
     * @throws GECommsException If something goes wrong (code or request related).
     */
    @Override
    public boolean postReddit(RPostPayload postPayload) throws GECommsException {
        // If online, ensure we have API keys
        if (this.rOnline) {
            if (this.env.getenv("REDDIT_API_CLIENT") == null) {
                throw new GECommsException(-1,
                        "Required environment variable REDDIT_API_CLIENT is missing.");
            } else if (this.env.getenv("REDDIT_API_SECRET") == null) {
                throw new GECommsException(-1,
                        "Required environment variable REDDIT_API_SECRET is missing.");
            }
        }

        // Check for token; if missing, not authenticated
        if (postPayload.token() == null) {
            throw new GECommsException(-1, "Trying to post with missing Reddit token.");
        }

        // Get post response
        GEResponse postResponse;

        if (this.rOnline) {
            postResponse = this.onlineComms.postReddit(postPayload);
        } else {
            postResponse = this.offlineComms.postReddit(postPayload);
        }

        // Check for errors
        if (postResponse.statusCode() >= 400 && postResponse.statusCode() <= 599) {
            throw new GECommsException(postResponse.statusCode(), "Error posting to Reddit.");
        }

        return true;
    }
}