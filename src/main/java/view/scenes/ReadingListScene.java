package view.scenes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import model.items.GContent;
import presenter.GEPresenter;
import view.util.GEViewCommon;
import view.util.GEViewLists;

public class ReadingListScene implements GEScene {
    private GEScene prev; // Keep reference to previous scene

    /**
     * Creates a reading list scene.
     * @param prev Previous scene (returning to it when pressing back).
     */
    public ReadingListScene(GEScene prev) {
        this.prev = prev;
    }

    /**
     * Returns the scene title.
     * @return Scene Title.
     */
    @Override
    public String getTitle() {
        return "Guardian-Email Client: Reading List";
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

        Label title = new Label("Your Reading List");
        title.setFont(new Font(24));

        ListView<GContent> resultsList = GEViewLists.contentList(p.getReadingList(), p);

        Label emptyIndicator = GEViewCommon.emptyListIndicator(resultsList);
        emptyIndicator.setVisible(resultsList.getItems().size() == 0);

        Button back = new Button("Back");
        back.setOnAction(event -> {
            p.setScene(prev);
        });

        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(title, emptyIndicator, resultsList, back);

        root.setCenter(center);

        return scene;
    }
}
