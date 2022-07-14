package presenter;

/**
 * Interface for observing a Presenter operation.
 */
@FunctionalInterface
public interface GEPresenterObserver {
    /**
     * Action to do when the Presenter calls it.
     */
    public void update();
}
