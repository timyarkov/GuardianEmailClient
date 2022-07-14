package view.scenes;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import model.items.GContent;
import model.items.GTag;
import presenter.GEPresenter;
import presenter.GEPresenterObserver;
import view.util.GEViewCommon;
import view.util.GEViewDialogs;
import view.util.GEViewLists;
import java.util.ArrayList;

/**
 * Scene containing all main functionality.
 */
public class MainScene implements GEScene {
    // For content navigation/selection
    private Integer[] currPage; // Single element array to act as pointer
    private Integer[] totalPages; // Single element array to act as pointer

    /**
     * Initialises a main scene.
     */
    public MainScene() {
        // Initialise page numbers to be placeholders for now
        this.currPage = new Integer[]{-1};
        this.totalPages = new Integer[]{-1};
    }

    // Utility Drawing Methods
    /**
     * Generates tag search field, which also handles when
     * content search box/results/output options are shown.
     * @param p Presenter.
     * @return VBox with the tag search field.
     */
    private VBox genTagSearch(GEPresenter p) {
        VBox elems = new VBox(12);

        VBox contentBox = new VBox(); // Pre-make box for content search items to go in

        // Main search bar
        HBox searchBar = new HBox(12);

        TextField searchField = new TextField();
        searchField.setPromptText("Search a tag");

        Button searchButton = new Button("Search Tags");

        searchBar.setAlignment(Pos.CENTER);
        searchBar.getChildren().addAll(searchField, searchButton);

        // Spinner for loading
        ImageView spinner = GEViewCommon.spinner(p);
        spinner.setDisable(true);

        // Autocomplete results
        ObservableList<GTag> suggestions = FXCollections.observableList(new ArrayList<>());
        ListView<GTag> suggestionsView = GEViewLists.tagsList(suggestions, p);

        // Empty list indicator
        Label emptyIndicator = GEViewCommon.emptyListIndicator(suggestionsView);
        emptyIndicator.setVisible(false);

        // Search Logic
        GEPresenterObserver preSearch = () -> {
            // Enable spinner, disable search items
            spinner.setDisable(false);
            searchField.setDisable(true);
            searchButton.setDisable(true);
        };

        GEPresenterObserver postSearch = () -> {
            // Disable spinner, enable search items
            spinner.setDisable(true);
            searchField.setDisable(false);
            searchButton.setDisable(false);
        };

        searchField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                contentBox.getChildren().clear(); // Clear out content search
                emptyIndicator.setVisible(false); // Hide empty indicator

                p.setTagsViewList(suggestionsView,
                                   searchField.getText(),
                                   preSearch,
                                   postSearch);
            }
        });

        searchButton.setOnAction(event -> {
            contentBox.getChildren().clear(); // Clear out content search
            emptyIndicator.setVisible(false); // Hide empty indicator

            p.setTagsViewList(suggestionsView,
                               searchField.getText(),
                               preSearch,
                               postSearch);
        });

        // Selection Logic
        suggestionsView.getSelectionModel()
                       .selectedItemProperty()
                       .addListener(((observable, oldValue, newValue) -> {
            if (newValue == null) {
                // We had an event, but no change in selected value
                return;
            }

            // Generate content search box for this tag
            GTag tag = suggestionsView.getSelectionModel().getSelectedItem();
            contentBox.getChildren().setAll(this.genContentSearch(p, tag));

            // Hide tag suggestions
            suggestionsView.setVisible(false);
            Platform.runLater(() -> suggestionsView.getSelectionModel().clearSelection());
        }));

        elems.setAlignment(Pos.CENTER);
        elems.getChildren().addAll(searchBar, spinner, emptyIndicator, suggestionsView, contentBox);

        return elems;
    }

    /**
     * Generates content search, page navigation and posting elements.
     * @param p Presenter.
     * @param tag Tag that was previously searched for.
     * @return VBox with content search elements.
     */
    private VBox genContentSearch(GEPresenter p, GTag tag) {
        VBox content = new VBox(12);

        // Search bar
        HBox searchBar = new HBox(12);

        TextField searchField = new TextField();
        searchField.setPromptText("Search for content with tag " + tag.id());
        searchField.setMinWidth(p.getXResolution() * 0.65);

        Button searchButton = new Button("Search Content");

        searchBar.setAlignment(Pos.CENTER);
        searchBar.getChildren().addAll(searchField, searchButton);

        // Results + Page Nav + Send email
        VBox resultsView = new VBox(12);
        resultsView.setVisible(false);
        // When not visible, don't account for it in layout
        /* The following methodology copied from https://stackoverflow.com/questions/28558165/javafx-setvisible-hides-the-element-but-doesnt-rearrange-adjacent-nodes */
        resultsView.managedProperty().bind(resultsView.visibleProperty());
        /* End of copied code */

        ImageView spinnerContent = GEViewCommon.spinner(p);
        spinnerContent.setDisable(true);

        // Results
        ObservableList<GContent> results = FXCollections.observableList(new ArrayList<>());
        ListView<GContent> resultsList = GEViewLists.contentList(results, p);
        // When not visible, don't account for it in layout
        /* The following methodology copied from https://stackoverflow.com/questions/28558165/javafx-setvisible-hides-the-element-but-doesnt-rearrange-adjacent-nodes */
        resultsList.managedProperty().bind(resultsList.visibleProperty());
        /* End of copied code */

        // Empty list indicator
        Label emptyIndicator = GEViewCommon.emptyListIndicator(resultsList);
        emptyIndicator.setVisible(false);

        // Page Nav
        HBox pageNav = new HBox(24);

        Button pageBack = new Button("< Prev Page");

        Label pageLabel = new Label("Page %d / %d".formatted(this.currPage[0],
                                                             this.totalPages[0]));

        Button pageForwards = new Button("Next Page >");

        pageNav.setAlignment(Pos.CENTER);
        pageNav.getChildren().addAll(pageBack, pageLabel, pageForwards);

        // Email + Reddit
        HBox emailRedditItems = new HBox(12);

        ImageView spinnerEmailReddit = GEViewCommon.spinner(p);
        spinnerEmailReddit.setDisable(true);

        Label emailRedditStatus = new Label("");
        // When not visible, don't account for it in layout
        /* The following methodology copied from https://stackoverflow.com/questions/28558165/javafx-setvisible-hides-the-element-but-doesnt-rearrange-adjacent-nodes */
        emailRedditStatus.managedProperty().bind(emailRedditStatus.visibleProperty());
        /* End of copied code */

        // Send/Post logic
        GEPresenterObserver preSendPost = () -> {
            // Enable spinner, disable field items
            spinnerEmailReddit.setDisable(false);
            emailRedditItems.setDisable(true);
        };

        GEPresenterObserver postSendPost = () -> {
            // Disable spinner, enable field items
            spinnerEmailReddit.setDisable(true);
            emailRedditItems.setDisable(false);
        };

        TextField sendEmailField = new TextField();
        sendEmailField.setPromptText("Email to send to");
        sendEmailField.setMinWidth(p.getXResolution() * 0.45);
        sendEmailField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                // Check for non-blank input first
                if (sendEmailField.getText().isBlank()) {
                    GEViewDialogs.badInput("email send", p);
                    return;
                }

                // Send email
                p.sendEmailConcurrent(tag,
                                       resultsList.getItems(),
                                       sendEmailField.getText(),
                                       emailRedditStatus,
                                       preSendPost,
                                       postSendPost);
            }
        });

        Button sendEmailButton = new Button("Output List to Email");

        sendEmailButton.setOnAction(event -> {
            // Check for non-blank input first
            if (sendEmailField.getText().isBlank()) {
                GEViewDialogs.badInput("email send", p);
                return;
            }

            // Send email
            p.sendEmailConcurrent(tag,
                                   resultsList.getItems(),
                                   sendEmailField.getText(),
                                   emailRedditStatus,
                                   preSendPost,
                                   postSendPost);
        });

        Button postRedditButton = new Button("Post to Reddit");

        postRedditButton.setOnAction(event -> {
            // Post
            p.postRedditConcurrent(tag,
                                    resultsList.getItems(),
                                    emailRedditStatus,
                                    preSendPost,
                                    postSendPost);
        });

        emailRedditItems.setAlignment(Pos.CENTER);
        emailRedditItems.getChildren().addAll(sendEmailField, sendEmailButton, postRedditButton);

        resultsView.setAlignment(Pos.CENTER);
        resultsView.getChildren().addAll(resultsList,
                                         pageNav,
                                         emailRedditStatus,
                                         spinnerEmailReddit,
                                         emailRedditItems);

        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(searchBar, emptyIndicator, spinnerContent, resultsView);

        // Search Logic
        GEPresenterObserver preSearch = () -> {
            // Enable spinner, disable search
            spinnerContent.setDisable(false);
            searchBar.setDisable(true);
            pageNav.setDisable(true);
            emailRedditItems.setDisable(true);
        };

        GEPresenterObserver postSearch = () -> {
            // Disable spinner, enable search
            spinnerContent.setDisable(true);
            searchBar.setDisable(false);
            pageNav.setDisable(false);
            emailRedditItems.setDisable(false);

            // Set nav stuff
            int reqCurrPage = 1;
            int reqTotalPages = 1;

            if (resultsList.getItems().size() > 0) {
                reqCurrPage = resultsList.getItems().get(0).pageNum();
                reqTotalPages = resultsList.getItems().get(0).totalPages();
            }

            pageForwards.setDisable(reqCurrPage == reqTotalPages);

            pageBack.setDisable(reqCurrPage == 1);

            pageLabel.setText("Page %d / %d".formatted(reqCurrPage, reqTotalPages));

            currPage[0] = reqCurrPage;
            totalPages[0] = reqTotalPages;
        };

        searchField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                emptyIndicator.setVisible(false); // Hide empty indicator

                // Get data
                p.setContentViewList(resultsList,
                                      tag,
                                      searchField.getText(),
                                      1,
                                      preSearch,
                                      postSearch);

                resultsView.setVisible(true); // Show results items
            }
        });

        searchButton.setOnAction(event -> {
            emptyIndicator.setVisible(false); // Hide empty indicator

            // Get data
            p.setContentViewList(resultsList,
                                  tag,
                                  searchField.getText(),
                                  1,
                                  preSearch,
                                  postSearch);

            resultsView.setVisible(true); // Show results items
        });

        // Page switch logic
        pageBack.setOnAction(event -> {
            emptyIndicator.setVisible(false); // Hide empty indicator

            // Decrement page, get data
            this.currPage[0] = this.currPage[0] - 1;

            p.setContentViewList(resultsList,
                                  tag,
                                  searchField.getText(),
                                  this.currPage[0],
                                  preSearch,
                                  postSearch);
        });

        pageForwards.setOnAction(event -> {
            emptyIndicator.setVisible(false); // Hide empty indicator

            // Increment page, get data
            this.currPage[0] = this.currPage[0] + 1;

            p.setContentViewList(resultsList,
                                  tag,
                                  searchField.getText(),
                                  this.currPage[0],
                                  preSearch,
                                  postSearch);
        });

        // When no items in list, disable email/posting
        resultsList.itemsProperty().addListener(observable -> {
            sendEmailField.setDisable(resultsList.getItems().size() == 0);
            sendEmailButton.setDisable(resultsList.getItems().size() == 0);
            postRedditButton.setDisable(resultsList.getItems().size() == 0);
        });

        return content;
    }

    // Primary Methods
    /**
     * Returns the scene title.
     * @return Scene Title.
     */
    @Override
    public String getTitle() {
        return "Guardian-Email Client";
    }

    /**
     * Constructs the scene.
     * @param p Presenter to use.
     */
    @Override
    public Scene makeScene(GEPresenter p) {
        // Setup
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, p.getXResolution(), p.getYResolution());
        scene.getStylesheets().add("global.css");

        // Top Menu
        MenuBar menu = GEViewCommon.menuBar(p);

        BorderPane.setMargin(menu, new Insets(6));

        root.setTop(menu);

        // Center Content
        VBox center = new VBox(18);

        Label title = new Label("Guardian-Email Client");
        title.setFont(new Font(24));
        title.getStyleClass().add("title");

        // Tag search gen will handle content search gen too
        VBox tagSearch = this.genTagSearch(p);

        Button readingListAccess = new Button("Articles to Read");
        readingListAccess.setOnAction(event -> {
            p.setScene(new ReadingListScene(this));
        });

        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(title, tagSearch, readingListAccess);

        root.setCenter(center);

        return scene;
    }
}