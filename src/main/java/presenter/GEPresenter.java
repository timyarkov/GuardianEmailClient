package presenter;

import javafx.application.HostServices;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import model.items.GContent;
import model.items.GTag;
import model.system.GESystem;
import model.system.GESystemObserver;
import view.scenes.GEScene;

import java.util.List;

/**
 * Manages view functionalities, including access to the model
 * and scene switching.
 */
public interface GEPresenter extends GESystemObserver {
    /**
     * Default X resolution.
     */
    public static final double DEFAULT_X = 600;

    /**
     * Default Y resolution.
     */
    public static final double DEFAULT_Y = 720;

    /**
     * Minimum X resolution.
     */
    public static final double MIN_X = 600;

    /**
     * Minimum Y resolution.
     */
    public static final double MIN_Y = 720;

    // System Operations
    /**
     * Returns the X resolution the view currently should be.
     * @return X resolution.
     */
    public double getXResolution();

    /**
     * Returns the Y resolution the view currently should be.
     * @return Y resolution.
     */
    public double getYResolution();

    /**
     * Sets the window resolution. Fails if below minimum dimensions.
     * @param x X resolution to set.
     * @param y Y resolution to set.
     * @return Whether set is successful or not.
     */
    public boolean setResolution(double x, double y);

    /**
     * Gets the stage to draw scenes on.
     * @return Stage.
     */
    public Stage getStage();

    /**
     * Gets application HostServices.
     * @return HostServices.
     */
    public HostServices getHostServices();

    /**
     * Sets the scene of the stage.
     * @param scene Scene to set.
     */
    public void setScene(GEScene scene);

    /**
     * Upon an update, the view manager check if an error state
     * has arisen in the program; if it has, it acts upon it.
     */
    @Override
    public void update();

    /**
     * Runs appropriate shutdown procedure.
     */
    public void shutdown();

    // Model Interactions
    // Guardian
    /**
     * Uses concurrency to get tags based on the query,
     * and then display them in the specified location.
     * @param location ListView to display results.
     * @param query Query for search.
     * @param preRequest Operation to do before the request.
     * @param postRequest Operation to do after the request.
     */
    public void setTagsViewList(ListView<GTag> location,
                                String query,
                                GEPresenterObserver preRequest,
                                GEPresenterObserver postRequest);

    /**
     * Requests that the model clear the cache.
     * @see GESystem#clearCache()
     */
    public void clearCache();

    /**
     * Uses concurrency to get content based on the tag/query/page combo, displaying
     * to the location after finishing.
     * Implicitly tries to use the cache.
     * @param location Location to display results.
     * @param tag Tag for search.
     * @param query Query for search.
     * @param page Page for search.
     * @param preRequest Operation to do before the request.
     * @param postRequest Operation to do after the request.
     */
    public void setContentViewList(ListView<GContent> location,
                                   GTag tag,
                                   String query,
                                   int page,
                                   GEPresenterObserver preRequest,
                                   GEPresenterObserver postRequest);

    // Email
    /**
     * Uses concurrency to make an email send request.
     * @param tag Tag of content being sent.
     * @param content Content to send.
     * @param recipient Recipient to send to.
     * @param indicator Indicator for email success/failure.
     * @param preRequest Operation to do before the request.
     * @param postRequest Operation to do after the request.
     */
    public void sendEmailConcurrent(GTag tag,
                                    List<GContent> content,
                                    String recipient,
                                    Label indicator,
                                    GEPresenterObserver preRequest,
                                    GEPresenterObserver postRequest);

    // Reddit
    /**
     * Posts to reddit as a background task, modifying
     * indicator when done to notify of results.
     * @param tag Tag to send.
     * @param content Content to send.
     * @param indicator Indicator to modify when done.
     * @param preRequest Operation to do before the request.
     * @param postRequest Operation to do after the request.
     */
    public void postRedditConcurrent(GTag tag,
                                     List<GContent> content,
                                     Label indicator,
                                     GEPresenterObserver preRequest,
                                     GEPresenterObserver postRequest);

    // Reading List
    /**
     * Returns the current article reading list from the system.
     * @return Current article reading list.
     */
    public List<GContent> getReadingList();

    /**
     * Passes content to the system for it to add to the reading list.
     * @param content Content to add.
     * @return If add was successful or not.
     */
    public boolean addToReadingList(GContent content);

    /**
     * Calls for the system to remove the specified content from the reading list.
     * @param content Content to remove.
     * @return If remove succeeded or not.
     */
    public boolean removeFromReadingList(GContent content);
}
