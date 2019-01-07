/*
 * Copyright (C) 2018 em
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.poulter.em.poker.client.test;

import javafx.application.Application;
import javafx.stage.Stage;
import space.poulter.em.poker.client.PokerTableDataClient;
import space.poulter.em.poker.client.PokerTableStage;

/**
 *
 * @author em
 */
public class PokerTableStageTest extends Application {
    public static void main(String args[]) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        PokerTableDataClient dat = new PokerTableDataClient();
        dat.init(1, 9);
        PokerTableStage newStage = new PokerTableStage(dat, null);
        //newStage.show();
        
        newStage.show();
    }
}


/*public class PokerTableStageTest extends Application {

    private static final Integer STARTTIME = 15;
    private Timeline timeline;
    private Label timerLabel = new Label();
    private IntegerProperty timeSeconds = new SimpleIntegerProperty(STARTTIME*100);


    /**
     * @param args the command line arguments
     
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("FX Timer Binding/ProgressBar");
        Group root = new Group();
        Scene scene = new Scene(root, 300, 250);

        // Bind the timerLabel text property to the timeSeconds property
        timerLabel.textProperty().bind(timeSeconds.divide(100).asString());
        timerLabel.setTextFill(Color.RED);
        timerLabel.setStyle("-fx-font-size: 4em;");
        
        ProgressBar progressBar = new ProgressBar();
        progressBar.progressProperty().bind(
                timeSeconds.divide(STARTTIME*100.0).subtract(1).multiply(-1));
        //progressBar.backgroundProperty().set(new Background(new BackgroundFill(Color.RED, null, null)));
        
        //Button button = new Button();
        //button.setText("Start Timer");
        primaryStage.setOnShown(new EventHandler<WindowEvent>() {
        //setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if (timeline != null) {
                    timeline.stop();
                }
                timeSeconds.set((STARTTIME+1)*100);
                timeline = new Timeline();
                timeline.getKeyFrames().add(
                        new KeyFrame(Duration.seconds(STARTTIME+1),
                        new KeyValue(timeSeconds, 0)));
                timeline.getKeyFrames().add(
                        new KeyFrame(Duration.seconds(STARTTIME-7),
                        new KeyValue(progressBar.styleProperty(), "-fx-accent: orange")));
                timeline.getKeyFrames().add(
                        new KeyFrame(Duration.seconds(STARTTIME-4),
                        new KeyValue(progressBar.styleProperty(), "-fx-accent: red")));
                //timeline.getKeyFrames().add(
                //        new KeyFrame(Duration.seconds(STARTTIME-5),
                //        new KeyValue(progressBar.backgroundProperty(), new Background(new BackgroundFill(Color.RED, null, null)))));
                timeline.playFromStart();
            }
        });
        
        //progressBar.backgroundProperty().set(new Background(new BackgroundFill(Color.RED, null, null)));
        
        VBox vb = new VBox(20);             // gap between components is 20
        vb.setAlignment(Pos.CENTER);        // center the components within VBox

        vb.setPrefWidth(scene.getWidth());
        vb.getChildren().addAll(timerLabel, progressBar);
        //vb.getChildren().addAll(progressBar);
        vb.setLayoutY(30);

        root.getChildren().add(vb);

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}*/
