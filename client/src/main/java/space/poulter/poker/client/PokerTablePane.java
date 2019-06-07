package space.poulter.poker.client;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import org.kordamp.ikonli.javafx.FontIcon;

class PokerTablePane extends StackPane {

    private Ellipse table;

    PokerTablePane(int tableId) {
        setBackground(new Background(new BackgroundFill(Color.DARKSLATEGREY, null, null)));
        setId("table_" + tableId);
        setMinWidth(500);
        setMinHeight(300);

        initTable();
        getChildren().add(table);

        Label tableIdLabel = new Label("Table " + tableId);
        tableIdLabel.translateXProperty().bind(widthProperty().divide(2).negate().add(30));
        tableIdLabel.translateYProperty().bind(heightProperty().divide(2).negate().add(30));
        getChildren().add(tableIdLabel);

        Button popOutButton = new Button();
        popOutButton.setBackground(null);
        popOutButton.setGraphic(new FontIcon("enty-popup"));
        popOutButton.setOnMouseClicked(event -> System.out.println("Button pressed"));
        popOutButton.translateXProperty().bind(widthProperty().divide(2).subtract(30));
        popOutButton.translateYProperty().bind(heightProperty().divide(2).negate().add(30));

        getChildren().add(popOutButton);
    }

    private void initTable() {
        table = new Ellipse();
        table.setFill(Color.GREEN);
        table.radiusXProperty().bind(widthProperty().divide(3));
        table.radiusYProperty().bind(heightProperty().divide(4));
        table.centerXProperty().bind(widthProperty().divide(2));
        table.centerYProperty().bind(heightProperty().divide(2));
    }


}
