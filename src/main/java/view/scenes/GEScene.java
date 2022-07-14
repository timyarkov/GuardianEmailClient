package view.scenes;

import javafx.scene.Scene;
import presenter.GEPresenter;

/**
 * Interface for an application scene.
 */
public interface GEScene {
    /**
     * Returns the scene title.
     * @return Scene Title.
     */
    public String getTitle();

    /**
     * Constructs the scene.
     * @param p Presenter to use.
     */
    public Scene makeScene(GEPresenter p);
}
