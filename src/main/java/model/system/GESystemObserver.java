package model.system;

/**
 * Interface for observers of a GESystem.
 */
public interface GESystemObserver {
    /**
     * Updates the observer; what this update entails is
     * at the discretion of the implementing class.
     */
    public void update();
}
