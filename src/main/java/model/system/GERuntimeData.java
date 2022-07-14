package model.system;

/**
 * Enum containing all possible fields for the system's runtime data.
 * There are no guarantees that all these fields won't be null.
 */
public enum GERuntimeData {
    REDDIT_TOKEN("reddit_token"),
    REDDIT_USERNAME("reddit_username");

    public final String key;

    private GERuntimeData(String key) {
        this.key = key;
    }
}
