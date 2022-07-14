package view.util;

import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import presenter.GEPresenter;

/**
 * Class with methods for creating complex but common
 * GUI elements.
 */
public class GEViewCommon {
    /**
     * Generates menubar common to scenes.
     * @param p Presenter to interact with.
     * @return Menubar.
     */
    public static MenuBar menuBar(GEPresenter p) {
        MenuBar menuBar = new MenuBar();

        // File; New session, clear cache and exit
        Menu file = new Menu("File");
        file.setStyle("-fx-text-fill: white");

        Menu switchTheme = new Menu("Set Theme");

        MenuItem setLight = new MenuItem("Light Theme");
        setLight.setOnAction(event -> {
            // Unapply dark style
            p.getStage().getScene().getStylesheets().remove("dark.css");
        });

        MenuItem setDark = new MenuItem("Dark Theme");
        setDark.setOnAction(event -> {
            // Apply dark style if not already applied
            if (!p.getStage().getScene().getStylesheets().contains("dark.css")) {
                p.getStage().getScene().getStylesheets().add("dark.css");
            }
        });

        switchTheme.getItems().addAll(setLight, setDark);

        MenuItem clearCache = new MenuItem("Clear Cache");
        clearCache.setOnAction(event -> {
            p.clearCache();
        });

        SeparatorMenuItem sep = new SeparatorMenuItem();

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(event -> {
            Platform.exit();
        });

        file.getItems().addAll(switchTheme, clearCache, sep, exit);

        // Help; About
        Menu help = new Menu("Help");

        MenuItem about = new MenuItem("About");
        about.setOnAction(event -> {
            GEViewDialogs.aboutDialog(p);
        });

        help.getItems().addAll(about);

        menuBar.getMenus().addAll(file, help);

        return menuBar;
    }

    /**
     * Makes an animated spinner.
     * @param p Presenter to interact with.
     * @return An animated spinner.
     */
    public static ImageView spinner(GEPresenter p) {
        Image img = new Image("loadspinner.png");
        ImageView spinner = new ImageView(img);

        spinner.setPreserveRatio(true);
        spinner.setFitHeight(p.getXResolution() * 0.05);

        // Animate spinning
        RotateTransition anim = new RotateTransition(Duration.millis(1000), spinner);
        anim.setFromAngle(0);
        anim.setToAngle(360);
        anim.setCycleCount(Animation.INDEFINITE);
        anim.play();

        // Make spinner invisible when disabled
        spinner.visibleProperty().bind(spinner.disableProperty().not());
        spinner.managedProperty().bind(spinner.disableProperty().not());

        return spinner;
    }

    /**
     * Makes a label indicating if an empty list; if list is empty
     * then it appears, if not empty then it is invisible and not
     * accounted for in layout.
     * Note that this implicitly adds the label as a listener to
     * the listview.
     * @param ls List to observe.
     * @return Empty list indicator.
     */
    public static Label emptyListIndicator(ListView<?> ls) {
        Label empty = new Label("No Results Found");

        empty.managedProperty().bind(empty.visibleProperty());

        // Make indicator visible for no items and empty otherwise
        ls.itemsProperty().addListener(observable -> {
            empty.setVisible(ls.getItems().size() == 0);
        });

        return empty;
    }
}
