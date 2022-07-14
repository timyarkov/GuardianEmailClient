package model.util;

/**
 * Wrapper for sleeping functionalities.
 */
public class SleepModule {
    /**
     * Does a sleep for the specified amount of time.
     * @param ms Milliseconds to sleep.
     * @return If sleep succeeded or not (i.e. was not interrupted, slept for full duration).
     */
    public boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
