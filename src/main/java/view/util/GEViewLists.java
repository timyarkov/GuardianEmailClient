package view.util;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import model.items.GContent;
import model.items.GTag;
import presenter.GEPresenter;
import java.util.List;

public class GEViewLists {
    /**
     * Generates a tags ListView.
     * @param tags Tags to display.
     * @param p Presenter to use.
     * @return Tags ListView.
     */
    public static ListView<GTag> tagsList(List<GTag> tags, GEPresenter p) {
        ListView<GTag> suggestionsView = new ListView<>(FXCollections.observableList(tags));
        suggestionsView.setCellFactory(param -> {
            ListCell<GTag> cell = new ListCell<GTag>() {
                @Override
                protected void updateItem(GTag item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText("");
                    } else {
                        // Create box with results
                        VBox result = new VBox(12);
                        result.getStyleClass().add("resultBox");
                        result.setMinWidth(suggestionsView.getWidth() * 0.9);
                        result.setMinHeight(suggestionsView.getHeight() * 0.15);
                        result.setMaxWidth(suggestionsView.getWidth() * 0.9);
                        result.setMaxHeight(suggestionsView.getHeight() * 0.1);

                        Label id = new Label(item.id());

                        result.setPadding(new Insets(12));
                        result.setAlignment(Pos.CENTER);
                        result.getChildren().addAll(id);

                        setAlignment(Pos.CENTER);
                        setGraphic(result);
                    }
                }
            };

            return cell;
        });
        suggestionsView.setMinWidth(p.getXResolution() * 0.95);
        suggestionsView.setMinHeight(p.getYResolution() * 0.4);
        suggestionsView.setMaxWidth(p.getXResolution() * 0.95);
        suggestionsView.setMaxHeight(p.getYResolution() * 0.4);
        suggestionsView.setVisible(false);
        // When not visible, don't account for it in layout
        /* The following methodology copied from https://stackoverflow.com/questions/28558165/javafx-setvisible-hides-the-element-but-doesnt-rearrange-adjacent-nodes */
        suggestionsView.managedProperty().bind(suggestionsView.visibleProperty());
        /* End of copied code */

        return suggestionsView;
    }

    /**
     * Generates a content ListView.
     * @param content Content to display.
     * @param p Presenter to use.
     * @return Content ListView.
     */
    public static ListView<GContent> contentList(List<GContent> content, GEPresenter p) {
        ListView<GContent> resultsList = new ListView<>(FXCollections.observableList(content));
        resultsList.setCellFactory(param -> {
            ListCell<GContent> cell = new ListCell<GContent>() {
                @Override
                protected void updateItem(GContent item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText("");
                        setGraphic(null);
                    } else {
                        // Create box with results
                        VBox result = new VBox(12);
                        result.getStyleClass().add("resultBox");
                        result.setMinWidth(resultsList.getWidth() * 0.9);
                        result.setMinHeight(resultsList.getHeight() * 0.2);
                        result.setMaxWidth(resultsList.getWidth() * 0.9);
                        result.setMaxHeight(resultsList.getHeight() * 0.35);

                        // Title + Info
                        Label title = new Label(item.webTitle());
                        title.setWrapText(true);
                        title.setTextAlignment(TextAlignment.CENTER);

                        Label info = new Label(item.sectionName() +
                                " - " +
                                item.webPublicationDate());

                        // Reading List status + actions
                        Button readingListAction; // Add or remove, depending on status

                        if (p.getReadingList().contains(item)) {
                            readingListAction = new Button("Remove from reading list");
                            readingListAction.setOnAction(event -> {
                                p.removeFromReadingList(item);
                                // Refresh list
                                resultsList.refresh();
                            });
                        } else {
                            readingListAction = new Button("Add to reading list");
                            readingListAction.setOnAction(event -> {
                                p.addToReadingList(item);
                                // Refresh list
                                resultsList.refresh();
                            });
                        }

                        // Opening in Browser
                        Button openBrowser = new Button("Open in browser");
                        openBrowser.setOnAction(event -> {
                            p.getHostServices().showDocument(item.webUrl());
                        });

                        HBox buttons = new HBox(12);
                        buttons.getChildren().addAll(readingListAction, openBrowser);
                        buttons.setAlignment(Pos.CENTER);

                        result.setPadding(new Insets(12));
                        result.setAlignment(Pos.CENTER);
                        result.getChildren().addAll(title, info, buttons);

                        setAlignment(Pos.CENTER);
                        setGraphic(result);
                    }
                }
            };

            return cell;
        });
        resultsList.setMinWidth(p.getXResolution() * 0.95);
        resultsList.setMinHeight(p.getYResolution() * 0.45);
        resultsList.setMaxWidth(p.getXResolution() * 0.95);
        resultsList.setMaxHeight(p.getYResolution() * 0.45);

        return resultsList;
    }
}
