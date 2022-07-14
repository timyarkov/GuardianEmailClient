package model.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import model.comms.exceptions.GECommsException;
import model.comms.manager.GECommsManager;
import model.comms.manager.GECommsManagerImpl;
import model.comms.payloads.*;
import model.db.GEDatabase;
import model.env.Environment;
import model.items.GContent;
import model.items.GTag;
import model.items.RedditToken;
import model.util.SleepModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static model.system.GERuntimeData.*;

/**
 * Implementation of the application system.
 * @see model.system.GESystem
 */
public class GESystemImpl implements GESystem {
    private Environment env;
    private GECommsManager comms;
    private List<GESystemObserver> observers;
    private boolean errorState;
    private String errorMsg;
    private ExecutorService pool;
    private SleepModule sleeper;

    // Data to be accessed at runtime/dynamically updated that are
    // not critical. See GERuntimeData for possible values
    private Map<String, String> runtimeData;

    private List<GContent> readingList;

    /**
     * Constructs the system.
     * @param gOnline Whether to use online API of The Guardian.
     * @param eOnline Whether to use online API of SendGrid.
     * @param rOnline Whether to use online API of Reddit.
     * @param db Database to use.
     */
    public GESystemImpl(boolean gOnline, boolean eOnline, boolean rOnline, GEDatabase db) {
        this.env = new Environment();
        this.comms = new GECommsManagerImpl(gOnline, eOnline, rOnline);
        this.comms.injectDatabase(db); // Inject Database
        this.observers = new ArrayList<>();
        this.errorState = false;
        this.errorMsg = null; // Initialise to null

        this.pool = Executors.newFixedThreadPool(2, runnable -> {
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            return t;
        });
        this.sleeper = new SleepModule();

        this.runtimeData = new HashMap<>();

        this.readingList = new ArrayList<>();
    }

    // Getter Methods
    /**
     * Returns a copy the current runtime data.
     * @see GERuntimeData For possible fields (which may or may not be populated).
     * @return Copy of the current runtime data.
     */
    @Override
    public Map<String, String> getRuntimeData() {
        return new HashMap<>(this.runtimeData);
    }

    // System modules
    /**
     * Injects a new comms manager. If null, the new comms manager will
     * not be set, and the previous one will be kept.
     * @param gecm Comms manager to inject.
     * @return If injection was successful or not.
     */
    @Override
    public boolean injectNewCommsManager(GECommsManager gecm) {
        if (gecm != null) {
            this.comms = gecm;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Injects a new environment. If null, the new environment will not
     * be set, and the previous one will be kept.
     * @param e Environment to inject.
     * @return If injection was successful or not.
     */
    @Override
    public boolean injectNewEnvironment(Environment e) {
        if (e != null) {
            this.env = e;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Injects a new sleep module. If null, the new sleep module will not
     * be set, and the previous one will be kept.
     * @param sm Sleep module to inject.
     * @return If injection was successful or not.
     */
    @Override
    public boolean injectNewSleepModule(SleepModule sm) {
        if (sm != null) {
            this.sleeper = sm;
            return true;
        } else {
            return false;
        }
    }

    // System State/Observation
    /**
     * Adds an observer to the system.
     * @param o Observer to add. Cannot be null.
     * @return Whether add was successful or not.
     */
    @Override
    public boolean addObserver(GESystemObserver o) {
        return o != null ? this.observers.add(o) : false;
    }

    /**
     * Removes an observer from the system.
     * @param o Observer to remove.
     * @return Whether remove was successful or not.
     */
    @Override
    public boolean removeObserver(GESystemObserver o) {
        return this.observers.remove(o);
    }

    /**
     * Broadcasts to all observers.
     */
    private void broadcast() {
        for (GESystemObserver o : this.observers) {
            o.update();
        }
    }

    /**
     * Returns whether an error has occurred or not. To be
     * utilised by observers after an update.
     * @return Whether system is in an error state or not.
     */
    @Override
    public boolean isErrorState() {
        return this.errorState;
    }

    /**
     * Sets the system error state
     * @param errorState Error state.
     */
    private void setErrorState(boolean errorState) {
        this.errorState = errorState;
    }

    /**
     * Returns system error message, if in an error state.
     * If system is not in an error state, this function will return
     * null if no previous error states were encountered, or the previous
     * error state.
     * @return Error message.
     */
    @Override
    public String getErrorMessage() {
        return this.errorMsg;
    }

    /**
     * Checks that all environment variables for the given arguments are present,
     * triggering an error state if any are missing.
     * @param guardian Whether to check for required Guardian API variables.
     * @param email Whether to check for required email API variables.
     * @return False if any variables are missing, True if all ok.
     */
    @Override
    public boolean checkEnvironmentVars(boolean guardian, boolean email) {
        boolean ok = true;

        if (guardian) {
            if (this.env.getenv("INPUT_API_KEY") == null) {
                this.screamError("Environment Variable INPUT_API_KEY is missing.\n" +
                        "Core functionality cannot work without this variable; " +
                        "please set it and try again.");
                ok = false;
            }
        }

        if (email) {
            if (this.env.getenv("SENDGRID_API_KEY") == null) {
                this.screamError("Environment Variable SENDGRID_API_KEY is missing.\n" +
                        "Core functionality cannot work without this variable; " +
                        "please set it and try again.");
                ok = false;
            }

            if (this.env.getenv("SENDGRID_API_EMAIL") == null) {
                this.screamError("Environment Variable SENDGRID_API_EMAIL is missing.\n" +
                        "Core functionality cannot work without this variable; " +
                        "please set it and try again.");
                ok = false;
            }
        }

        return ok;
    }

    // System Operations
    /**
     * Runs appropriate shutdown procedure.
     */
    @Override
    public void shutdown() {
        /* Code copied from https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html */
        pool.shutdownNow();

        try {
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow();

                if (pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.out.println("Pool did not terminate!");
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        /* End of copied code */
    }

    /**
     * Sets the error message.
     * @param errorMsg Error message.
     */
    private void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    /**
     * Sets error state and message, broadcasts, and then unsets
     * the error state.
     * @param errorMsg Error message
     */
    private void screamError(String errorMsg) {
        this.setErrorState(true);
        this.setErrorMsg(errorMsg);
        this.broadcast();
        this.setErrorState(false);
    }

    // The Guardian Operations
    /**
     * Gets tags from The Guardian API.
     * @param query Query to do. Cannot be null.
     * @return List of found guardian tags. Empty list if bad parameters or failure.
     */
    @Override
    public List<GTag> getTags(String query) {
        // Construct payload and then make request
        GTagPayload payload = new GTagPayload(query, 1, 10);

        try {
            JsonObject data = this.comms.getTags(payload).getAsJsonObject("response");
            JsonArray results = data.getAsJsonArray("results");

            List<GTag> ret = new ArrayList<>();

            for (JsonElement je : results) {
                JsonObject jo = je.getAsJsonObject();
                ret.add(new GTag(
                        jo.get("id").getAsString(),
                        jo.get("type").getAsString(),
                        jo.get("webTitle").getAsString(),
                        jo.get("webUrl").getAsString(),
                        jo.get("apiUrl").getAsString()
                ));
            }

            return ret;
        } catch (GECommsException | IllegalStateException | NullPointerException e) {
            // Set error state, return empty
            this.screamError("Tag getting error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Returns whether there is cached content for the given
     * tag/query/page combination. Essentially works as pass-through
     * to the comms manager.
     * @param tag   Tag of content.
     * @param query Query for content.
     * @param page  Page of content.
     * @return Whether there is cached content or not.
     * @see GECommsManager#isContentCached(GContentPayload) The method called.
     */
    @Override
    public boolean isCachedContent(GTag tag, String query, int page) {
        // Construct a payload, and send it
        GContentPayload payload = new GContentPayload(tag, query, page, 10);
        return this.comms.isContentCached(payload);
    }

    /**
     * Clears DB content cache. Essentially works as pass-through
     * to the comms manager.
     * @see GECommsManager#clearContentCache() The method called.
     */
    @Override
    public void clearCache() {
        if (!this.comms.clearContentCache()) {
            // Failure to clear cache
            this.screamError("Failed to clear content cache.");
        }
    }

    /**
     * Returns content from the Guardian API that matches the required tag.
     * @param tag Tag to filter by.
     * @param query Query to make.
     * @param page Page to search on.
     * @param useCache Whether to use cache, if available.
     * @return List of found guardian content with the matching tag.
     *         Empty list if bad parameters or failure.
     */
    @Override
    public List<GContent> getContent(GTag tag, String query, int page, boolean useCache) {
        // Construct payload and then make request
        GContentPayload payload = new GContentPayload(tag, query, page, 10);

        try {
            JsonObject data = this.comms.getContent(payload, useCache)
                                        .getAsJsonObject("response");
            JsonArray results = data.getAsJsonArray("results");

            int pageNum = data.get("currentPage").getAsInt();
            int totalPages = data.get("pages").getAsInt();

            List<GContent> ret = new ArrayList<>();

            for (JsonElement je : results) {
                JsonObject jo = je.getAsJsonObject();
                ret.add(new GContent(
                        jo.get("id").getAsString(),
                        jo.get("sectionId").getAsString(),
                        jo.get("sectionName").getAsString(),
                        jo.get("webPublicationDate").getAsString(),
                        jo.get("webTitle").getAsString(),
                        jo.get("webUrl").getAsString(),
                        jo.get("apiUrl").getAsString(),
                        pageNum,
                        totalPages
                ));
            }

            return ret;
        } catch (GECommsException | IllegalStateException | NullPointerException e) {
            // Set error state, return empty
            this.screamError("Content getting error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Email Operations
    /**
     * Sends an email with the tag and list of content from the Guardian API.
     * @param tag Tag to send.
     * @param content Content to send.
     * @param recipient Address to send email to.
     * @return Whether send was successful or not.
     */
    @Override
    public boolean sendEmail(GTag tag, List<GContent> content, String recipient) {
        // Construct Payload and make request
        ESendPayload payload = new ESendPayload(recipient, tag, content);

        try {
            return this.comms.sendEmail(payload);
        } catch (GECommsException e) {
            this.screamError("Email sending error: " + e.getMessage());
            return false;
        }
    }

    // Reddit Operations
    /**
     * Authenticates the reddit user. When authenticated, the token is saved to the
     * system and a thread is started counting down when the token expires.
     * When this thread finishes (i.e. expiry occurs), it sets the runtimeData
     * "reddit_token" to "INVALIDATED", broadcasts to observers, and then sets
     * "reddit_token" to null.
     * @param username Username to authentication with.
     * @param password Password to authentication with.
     * @return Whether authentication was successful or not.
     */
    @Override
    public boolean authenticateReddit(String username, String password) {
        // Construct payload, make request
        RTokenPayload payload = new RTokenPayload(username, password);
        RedditToken token;

        try {
            token = this.comms.getRedditToken(payload);
        } catch (GECommsException e) {
            this.screamError("Error Authenticating for Reddit: " + e.getMessage());
            return false;
        }

        // Save data
        this.runtimeData.put(REDDIT_TOKEN.key, token.token());
        this.runtimeData.put(REDDIT_USERNAME.key, username);

        // Start up invalidation thread
        RedditToken finalToken = token;
        Runnable invalidator = () -> {
            this.sleeper.sleep(finalToken.expiry() * 1000L);

            // Invalidate, update observers
            this.runtimeData.put(REDDIT_TOKEN.key, "INVALIDATED");
            this.broadcast();
            this.runtimeData.put(REDDIT_TOKEN.key, null);
        };

        this.pool.execute(invalidator);

        return true;
    }

    /**
     * Posts to reddit with the tag and list of content from the Guardian API.
     * Requires reddit authentication.
     * @param tag Tag to send.
     * @param content Content to send.
     * @return Whether send was successful or not.
     */
    @Override
    public boolean postReddit(GTag tag, List<GContent> content) {
        // Construct Payload and make request
        RPostPayload postPayload = new RPostPayload(this.getRuntimeData().get(REDDIT_USERNAME.key),
                                                    this.getRuntimeData().get(REDDIT_TOKEN.key),
                                                    tag,
                                                    content);

        try {
            return this.comms.postReddit(postPayload);
        } catch (GECommsException e) {
            this.screamError("Reddit posting error: " + e.getMessage());
            return false;
        }
    }

    // Reading List Operations
    /**
     * Returns the current article reading list.
     * @return Current article reading list.
     */
    @Override
    public List<GContent> getReadingList() {
        return this.readingList;
    }

    /**
     * Adds an item to the current reading list. Rejected if
     * null or a duplicate of something already in the list.
     * @param content Content to add.
     * @return If add was successful or not.
     */
    @Override
    public boolean addToReadingList(GContent content) {
        if (content == null || this.readingList.contains(content)) {
            return false;
        } else {
            return this.readingList.add(content);
        }
    }

    /**
     * Removes an item from the current reading list.
     * @param content Content to remove.
     * @return If remove succeeded or not.
     */
    @Override
    public boolean removeFromReadingList(GContent content) {
        return this.readingList.remove(content);
    }
}
