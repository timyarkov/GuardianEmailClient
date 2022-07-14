package model.env;

/**
 * Wrapper for accessing the runtime environment.
 * Solution inspired by:
 * <a href="https://stackoverflow.com/a/8169008">https://stackoverflow.com/a/8169008</a>
 */
public class Environment {
    /**
     * Gets the environment variable requested, via System.getenv().
     * @see System#getenv()
     * @param name Environment variable to search for.
     * @return String of environment variable, null if doesn't exist.
     */
    public String getenv(String name) {
        return System.getenv(name);
    }
}
