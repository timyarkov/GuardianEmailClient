package model.db;

import model.db.exceptions.GEDatabaseException;
import model.items.GTag;

import java.sql.*;

/**
 * Implementation of GEDatabase using SQLite.
 * @see GEDatabase
 */
public class GEDatabaseSQLite implements GEDatabase {
    private static final String DB_SUFFIX = "jdbc:sqlite:";

    private String dbPath; // Path to DB file
    private String dbUrl; // URL to use

    /**
     * Constructs a GEDatabase for SQLite, also
     * establishing a connection (creating one if it doesn't
     * exist).
     * @param path Path to database.
     * @throws GEDatabaseException If connection cannot be established (critical error).
     */
    public GEDatabaseSQLite(String path) {
        this.dbPath = path;
        this.dbUrl = DB_SUFFIX + dbPath;

        if (!this.setupDB()) { // Set up the DB
            throw new GEDatabaseException(true, "Failed to set up connection.");
        }
    }

    // Core Methods
    /**
     * Sets up the database connection, ensuring that it can be made;
     * i.e. if the DB file doesn't exist, creates it, else ensures
     * a connection can be made.
     * @return Whether successful or not.
     */
    @Override
    public boolean setupDB() {
        // Setup tables
        String createCacheTable =
                """
                CREATE TABLE IF NOT EXISTS ContentCache (
                    tag_id TEXT NOT NULL,
                    query TEXT NOT NULL,
                    page INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    PRIMARY KEY (tag_id, query, page)
                );
                """;

        try (Connection c = DriverManager.getConnection(dbUrl);
             Statement s = c.createStatement()) {
            s.execute(createCacheTable);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

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
    public boolean cacheContent(GTag tag, String query, int page, String jsonContent) {
        // Error Checking
        if (tag == null) {
            return false;
        } else if (tag.id() == null) {
            return false;
        } else if (query == null) {
            return false;
        } else if (page <= 0) {
            return false;
        } else if (jsonContent == null) {
            return false;
        }

        String addQuery =
                """
                INSERT OR REPLACE INTO ContentCache VALUES (
                    ?,
                    ?,
                    ?,
                    ?
                )
                """;

        try (Connection c = DriverManager.getConnection(dbUrl);
             PreparedStatement s = c.prepareStatement(addQuery)) {
            // Set params
            s.setString(1, tag.id());
            s.setString(2,query);
            s.setInt(3,page);
            s.setString(4, jsonContent);

            s.execute();

            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Returns string of content cached for the tag (in JSON format).
     * @param tag Tag for content. Cannot be null.
     * @param query Query for content. Cannot be null.
     * @param page Page to cache. Has to be >= 1.
     * @return String of content cached. If nothing found, returns an empty
     *         string. If bad parameters or an error, returns null.
     */
    @Override
    public String getCachedContent(GTag tag, String query, int page) {
        if (tag == null) {
            return null;
        } else if (tag.id() == null) {
            return null;
        } else if (query == null) {
            return null;
        } else if (page <= 0) {
            return null;
        }

        String search =
                """
                SELECT content
                FROM ContentCache
                WHERE tag_id = ? AND query = ? AND page = ?;
                """;

        try (Connection c = DriverManager.getConnection(dbUrl);
             PreparedStatement s = c.prepareStatement(search)) {
            // Set params
            s.setString(1, tag.id());
            s.setString(2, query);
            s.setInt(3, page);

            ResultSet res = s.executeQuery();

            if (res.next()) {
                // Success! Data is present
                return res.getString("content");
            } else {
                // No data found
                return "";
            }
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Clears the content cache.
     * @return Whether clear was successful or not.
     */
    public boolean clearCachedContent() {
        String clearQuery =
                """
                DELETE FROM ContentCache;
                """;

        try (Connection c = DriverManager.getConnection(dbUrl);
             Statement s = c.createStatement()) {
            s.execute(clearQuery);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
