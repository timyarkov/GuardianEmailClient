package view.util;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import model.items.GTag;
import model.system.GESystem;
import presenter.GEPresenter;

import java.util.HashMap;
import java.util.Map;

/**
 * Class with methods for creating dialogs.
 */
public class GEViewDialogs {
    /**
     * Creates an error dialog based on the current error state
     * of the system passed in.
     * @param ges System to get error state info from.
     * @param p Presenter to interact with.
     */
    public static void errorDialog(GESystem ges, GEPresenter p) {
        Alert err = new Alert(Alert.AlertType.ERROR);
        err.getDialogPane().getStylesheets().setAll(p.getStage().getScene().getStylesheets());
        err.setTitle("System Error");
        err.setHeaderText("Something went wrong!");

        String errText = ges.getErrorMessage();

        err.setContentText(errText != null ? errText : "Erroneous call for error dialog; " +
                                                       "no error message set.");

        err.showAndWait();
    }

    /**
     * Makes a dialog prompting the user to choose whether
     * to use cached content or not.
     * @param tag Tag of cached content.
     * @param query Query for cached content.
     * @param page Page of cached content.
     * @param p Presenter to interact with.
     * @return The user's choice of whether to use the cache.
     */
    public static boolean useCacheDialog(GTag tag, String query, int page, GEPresenter p) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.getDialogPane().getStylesheets().setAll(p.getStage().getScene().getStylesheets());
        dialog.setTitle("Cached Data Available");
        dialog.setHeaderText(("Cached data available for tag %s " +
                "and query %s (page %d)").formatted(tag.id(), query, page));
        dialog.setContentText("Would you like to use this cached data, " +
                "or request fresh data from the API?");

        // Setup button types
        dialog.getButtonTypes().clear();

        /* Approach adapted from https://openjfx.io/javadoc/18/javafx.controls/javafx/scene/control/Dialog.html */
        ButtonType useCache = new ButtonType("Use Cache", ButtonBar.ButtonData.YES);
        ButtonType freshData = new ButtonType("Request Fresh Data", ButtonBar.ButtonData.NO);

        dialog.getButtonTypes().addAll(useCache, freshData);

        Boolean[] ret = new Boolean[]{true};

        dialog.showAndWait().ifPresent(response -> {
            if (response == useCache) {
                ret[0] = true;
            } else if (response == freshData) {
                ret[0] = false;
            } else {
                ret[0] = false; // Default to false
            }
        });
        /* End of adapted code */

        return ret[0];
    }

    /**
     * Creates a dialog with "about" information.
     * @param p Presenter to interact with.
     */
    public static void aboutDialog(GEPresenter p) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.getDialogPane().getStylesheets().setAll(p.getStage().getScene().getStylesheets());
        info.setTitle("Guardian-Email Client");
        info.setHeaderText("Guardian-Email Client v0.1");
        info.setContentText(
                """
                Search for tags and content for a tag on the Guardian, and then send the results to yourself via email.
                Posting to Reddit is also supported.
                
                
                Author: Tim Yarkov
                
                
                References:
                https://stackoverflow.com/questions/28558165/javafx-setvisible-hides-the-element-but-doesnt-rearrange-adjacent-nodes
                
                https://openjfx.io/javadoc/18/javafx.controls/javafx/scene/control/Dialog.html
                
                https://stackoverflow.com/a/8169008
                
                https://stackoverflow.com/a/49567500
                
                https://docs.oracle.com/javase/7/docs/technotes/guides/net/http-auth.html
                
                https://hg.openjdk.java.net/openjfx/8u40/rt/file/6cc08ec1ea82/modules/controls/src/main/resources/com/sun/javafx/scene/control/skin/modena/modena.css
                
                https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
                
                Various examples from the JavaFX documentation: https://openjfx.io/javadoc/18/
                
                JavaFX CSS Reference Guide: https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html
                """);

        info.show();
    }

    /**
     * Creates a dialog notifying of bad input.
     * @param field Field with bad input.
     * @param p Presenter to interact with.
     */
    public static void badInput(String field, GEPresenter p) {
        Alert badInput = new Alert(Alert.AlertType.WARNING);
        badInput.getDialogPane().getStylesheets().setAll(p.getStage().getScene().getStylesheets());
        badInput.setHeaderText("Invalid input in %s field!".formatted(field));
        badInput.setContentText("Please give a non-blank input.");
        badInput.showAndWait();
    }

    /**
     * Creates a login prompt.
     * @param reason Reason for login.
     * @param p Presenter to interact with.
     * @return Returns a map with key "cancelled" of value either "no" or "yes";
     *         if "cancelled" is "no", then there will also be keys "username"
     *         and "password".
     */
    public static Map<String, String> login(String reason, GEPresenter p) {
        Map<String, String> ret = new HashMap<>();

        ret.put("cancelled", "yes"); // Initialise indicator for failure

        // Make the dialog
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.getDialogPane().getStylesheets().setAll(p.getStage().getScene().getStylesheets());
        dialog.setTitle("Login");
        dialog.setHeaderText("Please provide login details for " + reason);
        ButtonType login = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().clear(); // Remove default OK button
        dialog.getDialogPane().getButtonTypes().add(login);

        // Login content
        VBox content = new VBox(24);

        // Username
        HBox username = new HBox(12);

        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.requestFocus();

        username.setAlignment(Pos.CENTER);
        username.getChildren().addAll(usernameLabel, usernameField);

        // Password
        HBox password = new HBox(12);

        Label passwordLabel = new Label("Password:");
        TextField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        password.setAlignment(Pos.CENTER);
        password.getChildren().addAll(passwordLabel, passwordField);

        // Notice
        Label notice = new Label("You will not have to login again for " +
                                 "the rest of this session (or until authentication " +
                                 "expiry, whichever comes first) if successful.");
        notice.setWrapText(true);
        notice.setTextAlignment(TextAlignment.CENTER);

        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(username, password, notice);

        dialog.getDialogPane().setContent(content);

        // Make login button unavailable for empty fields
        dialog.getDialogPane().lookupButton(login).setDisable(true);

        EventHandler<KeyEvent> loginDisable = event -> {
            dialog.getDialogPane()
                  .lookupButton(login)
                  .setDisable(usernameField.getText().isBlank() ||
                              passwordField.getText().isBlank());
        };

        usernameField.setOnKeyTyped(loginDisable);
        passwordField.setOnKeyTyped(loginDisable);

        // Show dialog, get results
        dialog.showAndWait().ifPresent(response -> {
            if (response == login) {
                ret.put("username", usernameField.getText());
                ret.put("password", passwordField.getText());
                ret.put("cancelled", "no"); // We didn't cancel!
            }
        });

        return ret;
    }

    /**
     * Creates a dialog notifying of an invalidated reddit token.
     * @param p Presenter to interact with.
     */
    public static void invalidatedRedditTokenDialog(GEPresenter p) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.getDialogPane().getStylesheets().setAll(p.getStage().getScene().getStylesheets());
        info.setTitle("Reddit Authentication Expired");
        info.setHeaderText("Your Reddit authentication expired.");
        info.setContentText("Please login the next time you wish to use Reddit posting.");

        info.show();
    }
}
