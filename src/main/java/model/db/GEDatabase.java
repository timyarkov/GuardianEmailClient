package model.db;

import model.items.GTag;

/**
 * Interface for interactions with a database.
 */
public interface GEDatabase {
    // Core Methods
    /**
     * Sets up the database connection, ensuring that it can be made.
     * @return Whether successful or not.
     */
    public boolean setupDB();

    // GE Methods
    /**
     * Caches content for a given tag and page. If cached content
     * exists, it should be overwritten.
     * @param tag  Tag for content. Cannot be null.
     * @param query Query to cache for. Cannot be null.
     * @param page Page to cache. Has to be >= 1.
     * @param jsonContent Content to cache. Cannot be null.
     * @return Whether the operation was successful or not.
     */
    public boolean cacheContent(GTag tag, String query, int page, String jsonContent);

    /**
     * Returns string of content cached for the tag (in JSON format).
     * @param tag Tag for content. Cannot be null.
     * @param query Query for content. Cannot be null.
     * @param page Page to cache. Has to be >= 1.
     * @return String of content cached. If nothing found, returns an empty
     *         string. If bad parameters or an error, returns null.
     */
    public String getCachedContent(GTag tag, String query, int page);

    /**
     * Clears the content cache.
     * @return Whether clear was successful or not.
     */
    public boolean clearCachedContent();
}
