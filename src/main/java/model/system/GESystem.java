package model.system;

import model.comms.manager.GECommsManager;
import model.comms.payloads.GContentPayload;
import model.env.Environment;
import model.items.GContent;
import model.items.GTag;
import model.util.SleepModule;

import java.util.List;
import java.util.Map;

/**
 * Contact-point for Guardian-Email Application System.
 */
public interface GESystem {
    // Getter Methods
    /**
     * Returns the runtime data.
     * @return Runtime data.
     */
    public Map<String, String> getRuntimeData();

    // System modules
    /**
     * Injects a new comms manager. If null, the new comms manager will
     * not be set, and the previous one will be kept.
     * @param gecm Comms manager to inject.
     * @return If injection was successful or not.
     */
    public boolean injectNewCommsManager(GECommsManager gecm);

    /**
     * Injects a new environment. If null, the new environment will not
     * be set, and the previous one will be kept.
     * @param e Environment to inject.
     * @return If injection was successful or not.
     */
    public boolean injectNewEnvironment(Environment e);

    /**
     * Injects a new sleep module. If null, the new sleep module will not
     * be set, and the previous one will be kept.
     * @param sm Sleep module to inject.
     * @return If injection was successful or not.
     */
    public boolean injectNewSleepModule(SleepModule sm);

    // System State/Observation
    /**
     * Adds an observer to the system.
     * @param o Observer to add. Cannot be null.
     * @return Whether add was successful or not.
     */
    public boolean addObserver(GESystemObserver o);

    /**
     * Removes an observer from the system.
     * @param o Observer to remove.
     * @return Whether remove was successful or not.
     */
    public boolean removeObserver(GESystemObserver o);

    /**
     * Returns whether an error has occurred or not. To be
     * utilised by observers after an update.
     * @return Whether system is in an error state or not.
     */
    public boolean isErrorState();

    /**
     * Returns system error message, if in an error state.
     * If system is not in an error state, this function will return
     * null if no previous error states were encountered, or the previous
     * error state.
     * @return Error message.
     */
    public String getErrorMessage();

    /**
     * Checks that all environment variables for the given arguments are present,
     * triggering an error state if any are missing.
     * @param guardian Whether to check for required Guardian API variables.
     * @param email Whether to check for required email API variables.
     * @return False if any variables are missing, True if all ok.
     */
    public boolean checkEnvironmentVars(boolean guardian, boolean email);

    // System Operations
    /**
     * Runs appropriate shutdown procedure.
     */
    public void shutdown();

    // The Guardian Operations
    /**
     * Gets tags from The Guardian API.
     * @param query Query to do.
     * @return List of found guardian tags. Empty list if bad parameters or failure.
     */
    public List<GTag> getTags(String query);

    /**
     * Returns whether there is cached content for the given
     * tag/query/page combination. Essentially works as pass-through
     * to the comms manager.
     * @param tag Tag of content.
     * @param query Query for content.
     * @param page Page of content.
     * @see GECommsManager#isContentCached(GContentPayload) The method called.
     * @return Whether there is cached content or not.
     */
    public boolean isCachedContent(GTag tag, String query, int page);

    /**
     * Clears DB content cache. Essentially works as pass-through
     * to the comms manager.
     * @see GECommsManager#clearContentCache() The method called.
     */
    public void clearCache();

    /**
     * Returns content from the Guardian API that matches the required tag.
     * @param tag Tag to filter by.
     * @param query Query to make.
     * @param page Page to search on.
     * @param useCache Whether to use cache, if available.
     * @return List of found guardian content with the matching tag.
     *         Empty list if bad parameters or failure.
     */
    public List<GContent> getContent(GTag tag, String query, int page, boolean useCache);

    // Email Operations
    /**
     * Sends an email with the tag and list of content from the Guardian API.
     * @param tag Tag to send.
     * @param content Content to send.
     * @param recipient Address to send email to.
     * @return Whether send was successful or not.
     */
    public boolean sendEmail(GTag tag, List<GContent> content, String recipient);

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
    public boolean authenticateReddit(String username, String password);

    /**
     * Posts to reddit with the tag and list of content from the Guardian API.
     * @param tag Tag to send.
     * @param content Content to send.
     * @return Whether send was successful or not.
     */
    public boolean postReddit(GTag tag, List<GContent> content);

    // Reading List Operations
    /**
     * Returns the current article reading list.
     * @return Current article reading list.
     */
    public List<GContent> getReadingList();

    /**
     * Adds an item to the current reading list. Rejected if
     * null or a duplicate of something already in the list.
     * @param content Content to add.
     * @return If add was successful or not.
     */
    public boolean addToReadingList(GContent content);

    /**
     * Removes an item from the current reading list.
     * @param content Content to remove.
     * @return If remove succeeded or not.
     */
    public boolean removeFromReadingList(GContent content);
}
