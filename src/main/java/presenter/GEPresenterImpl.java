package presenter;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import model.items.GContent;
import model.items.GTag;
import model.system.GERuntimeData;
import model.system.GESystem;
import model.system.GESystemObserver;
import view.scenes.GEScene;
import view.util.GEViewDialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a view manager.
 * @see GEPresenter
 */
public class GEPresenterImpl implements GESystemObserver, GEPresenter {
    private Stage stage;
    private GESystem ges;
    private double xRes;
    private double yRes;
    private HostServices hs;

    // Concurrency
    private ExecutorService pool;

    /**
     * Creates a view manager.
     * @param stage Stage to use.
     * @param ges System to use (model).
     * @param hs HostServices object.
     */
    public GEPresenterImpl(Stage stage, GESystem ges, HostServices hs) {
        this.stage = stage;
        this.ges = ges;
        this.xRes = DEFAULT_X;
        this.yRes = DEFAULT_Y;
        this.hs = hs;

        this.pool = Executors.newFixedThreadPool(2, runnable -> {
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            return t;
        });

        // Add an observer to notify when reddit token is invalidated
        GESystemObserver redditInvalidationObserver = () -> {
            if (ges.getRuntimeData().containsKey(GERuntimeData.REDDIT_TOKEN.key)) {
                if (ges.getRuntimeData()
                       .get(GERuntimeData.REDDIT_TOKEN.key).equals("INVALIDATED")) {
                    Platform.runLater(() -> {
                        GEViewDialogs.invalidatedRedditTokenDialog(this);
                    });
                }
            }
        };
        this.ges.addObserver(redditInvalidationObserver);
    }

    /**
     * Returns the X resolution the view currently should be.
     * @return X resolution.
     */
    @Override
    public double getXResolution() {
        return this.xRes;
    }

    /**
     * Returns the Y resolution the view currently should be.
     * @return Y resolution.
     */
    @Override
    public double getYResolution() {
        return this.yRes;
    }

    /**
     * Sets the window resolution. Fails if below minimum dimensions.
     * @param x X resolution to set.
     * @param y Y resolution to set.
     * @return Whether set is successful or not.
     */
    @Override
    public boolean setResolution(double x, double y) {
        if (x < MIN_X || y < MIN_Y) {
            return false;
        }

        this.xRes = x;
        this.yRes = y;

        return true;
    }

    /**
     * Gets the stage to draw scenes on.
     * @return Stage.
     */
    @Override
    public Stage getStage() {
        return this.stage;
    }

    /**
     * Gets the model for the view to interact with.
     * @return System model.
     */
    private GESystem getModel() {
        return this.ges;
    }

    /**
     * Gets application HostServices.
     * @return HostServices.
     */
    @Override
    public HostServices getHostServices() {
        return this.hs;
    }

    /**
     * Sets the scene of the stage.
     * @param scene Scene to set.
     */
    @Override
    public void setScene(GEScene scene) {
        // Save old stylesheets to apply to new scene if applicable
        ObservableList<String> prevSceneStyle = null;

        if (this.getStage().getScene() != null) {
            prevSceneStyle = this.getStage().getScene().getStylesheets();
        }

        // Switch
        this.getStage().setScene(scene.makeScene(this));
        this.getStage().setTitle(scene.getTitle());

        // Apply previous scene style
        if (prevSceneStyle != null) {
            this.getStage().getScene().getStylesheets().setAll(prevSceneStyle);
        }
    }

    /**
     * Upon an update, the view manager check if an error state
     * has arisen in the program; if it has, it acts upon it.
     */
    @Override
    public void update() {
        // Check for error state, if yes create an alert
        if (this.getModel().isErrorState()) {
            Platform.runLater(() -> {
                GEViewDialogs.errorDialog(this.getModel(), this);
            });
        }
    }

    /**
     * Runs appropriate shutdown procedure.
     * In this implementation, this involves shutting down the
     * thread pool.
     */
    @Override
    public void shutdown() {
        /* Code copied from https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html */
        pool.shutdown();

        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();

                if (pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.out.println("Pool did not terminate!");
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        /* End of copied code */
    }

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
    @Override
    public void setTagsViewList(ListView<GTag> location,
                                String query,
                                GEPresenterObserver preRequest,
                                GEPresenterObserver postRequest) {
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() {
                Platform.runLater(() -> {
                    // Hide listview while its being worked on
                    location.setVisible(false);

                    // Run pre request operations
                    preRequest.update();
                });

                List<GTag> tags = getModel().getTags(query);

                Platform.runLater(() -> {
                    location.setItems(FXCollections.observableList(tags));

                    // Reveal results, if any
                    if (location.getItems().size() != 0) {
                        location.setVisible(true);
                    }

                    // Run post request operations
                    postRequest.update();
                });

                return true;
            }
        };

        pool.execute(task);
    }

    /**
     * Requests that the model clear the cache.
     * @see GESystem#clearCache()
     */
    @Override
    public void clearCache() {
        this.getModel().clearCache();
    }

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
    @Override
    public void setContentViewList(ListView<GContent> location,
                                   GTag tag,
                                   String query,
                                   int page,
                                   GEPresenterObserver preRequest,
                                   GEPresenterObserver postRequest) {
        GEPresenter p = this; // Get reference to self

        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                List<GContent> content;

                Platform.runLater(() -> {
                    // Hide listview while working
                    location.setVisible(false);

                    // Run pre-request operations
                    preRequest.update();
                });

                if (getModel().isCachedContent(tag, query, page)) {
                    Boolean[] useCache = new Boolean[]{null};

                    Platform.runLater(() -> {
                        useCache[0] = GEViewDialogs.useCacheDialog(tag, query, page, p);
                    });

                    // Wait for decision
                    while (useCache[0] == null) {
                        Thread.sleep(1);
                    }

                    content = getModel().getContent(tag, query, page, useCache[0]);
                } else {
                    content = getModel().getContent(tag, query, page, true);
                }

                Platform.runLater(() -> {
                    location.setItems(FXCollections.observableList(content));

                    // Reveal results, if any
                    if (location.getItems().size() != 0) {
                        location.setVisible(true);
                    }

                    // Run post-request operations
                    postRequest.update();
                });

                return true;
            }
        };

        pool.execute(task);
    }

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
    @Override
    public void sendEmailConcurrent(GTag tag,
                                    List<GContent> content,
                                    String recipient,
                                    Label indicator,
                                    GEPresenterObserver preRequest,
                                    GEPresenterObserver postRequest) {
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() {
                Platform.runLater(() -> {
                    // Hide indicator while working
                    indicator.setVisible(false);

                    // Run pre-request operations
                    preRequest.update();
                });

                boolean success = getModel().sendEmail(tag, content, recipient);

                Platform.runLater(() -> {
                    // Set label based on result
                    if (success) {
                        indicator.setText("Success sending email!");
                    } else {
                        indicator.setText("Failure, email not sent");
                    }

                    // Show indicator
                    indicator.setVisible(true);

                    // Run post-request operations
                    postRequest.update();
                });

                return true;
            }
        };

        pool.execute(task);
    }

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
                                     GEPresenterObserver postRequest) {
        GEPresenter p = this; // Get reference to self

        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                Platform.runLater(() -> {
                    // Hide indicator while working
                    indicator.setVisible(false);

                    // Run pre-request operations
                    preRequest.update();
                });

                boolean authSuccess = true;

                // Authenticate if needed
                if (getModel().getRuntimeData().get(GERuntimeData.REDDIT_TOKEN.key) == null) {
                    List<Map<String, String>> loginDetails = new ArrayList<>();
                    loginDetails.add(null);

                    Platform.runLater(() -> {
                        loginDetails.set(0, GEViewDialogs.login("Reddit", p));
                    });

                    // Wait for decision
                    while (loginDetails.get(0) == null) {
                        Thread.sleep(1);
                    }

                    // Check for cancelled; if not, login
                    if (loginDetails.get(0).get("cancelled").equals("no")) {
                        authSuccess = getModel().authenticateReddit(
                                loginDetails.get(0).get("username"),
                                loginDetails.get(0).get("password")
                        );
                    } else {
                        authSuccess = false;
                    }
                }

                boolean postSuccess;

                // If successfully authenticated, post
                if (authSuccess) {
                    postSuccess = getModel().postReddit(tag, content);
                } else {
                    postSuccess = false;
                }

                Platform.runLater(() -> {
                    // Set label based on result
                    if (postSuccess) {
                        indicator.setText("Success posting to Reddit!");
                    } else {
                        indicator.setText("Failure, post not made");
                    }

                    // Show indicator
                    indicator.setVisible(true);

                    // Run post-request operations
                    postRequest.update();
                });

                return true;
            }
        };

        pool.execute(task);
    }

    // Reading List
    /**
     * Returns the current article reading list from the system.
     * @return Current article reading list.
     */
    @Override
    public List<GContent> getReadingList() {
        return this.getModel().getReadingList();
    }

    /**
     * Passes content to the system for it to add to the reading list.
     * @param content Content to add.
     * @return If add was successful or not.
     */
    @Override
    public boolean addToReadingList(GContent content) {
        return this.getModel().addToReadingList(content);
    }

    /**
     * Calls for the system to remove the specified content from the reading list.
     * @param content Content to remove.
     * @return If remove succeeded or not.
     */
    @Override
    public boolean removeFromReadingList(GContent content) {
        return this.getModel().removeFromReadingList(content);
    }
}
