/*
 * Copyright (C) 2018 Em Poulter <em@poulter.space>
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

package space.poulter.poker.client;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import space.poulter.poker.*;
import space.poulter.poker.client.OldPokerClient.ClientSideSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/* TODO - this class is really disgusting and messy, and it needs a lot of refactoring. */

/**
 * @author Em Poulter
 */
public class PokerTableStage extends Stage {

    private final Integer TIMERLENGTH = 20;
    private final ClientSideSocket socket;
    private Ellipse table;
    private Text messageText;
    private BoardRegion board;
    private Text potText;
    private BiMap<Integer, TableSection> seats;
    private TableSection playerSeat;
    private Button standUpButton;
    private HBox actionButtons;
    private ProgressBar timer;
    private Timeline timeline;
    private PokerTableDataClient data;

    /**
     * Creates a new poker table, with the information from dat.
     *
     * @param dat  The data with which to construct the table.
     * @param sock The socket through which the stage may interact with the server
     */
    PokerTableStage(PokerTableDataClient dat, ClientSideSocket sock) {
        socket = sock;
        data = dat;
        playerSeat = null;
        init();
    }

    /**
     * Initialises all of the graphics for the stage.
     */
    private void init() {
        /* Set the width/height of the stage */
        int startHeight = 500;
        int startWidth = 800;

        /* Create a new stackpane with dark gray background */
        StackPane backPane = new StackPane();

        BackgroundFill backfill = new BackgroundFill(Color.DARKSLATEGREY, null, null);
        backPane.setBackground(new Background(backfill));

        /* Initialise all of the components */
        initTable();
        initSeats();
        initButtons();
        initTimer();

        /* Add all of the components to the pane */
        backPane.getChildren().add(table);
        for (Map.Entry<Integer, TableSection> seat : seats.entrySet()) {
            backPane.getChildren().add(seat.getValue().sr);
            backPane.getChildren().add(seat.getValue().hr);
            backPane.getChildren().add(seat.getValue().chipCountText);
            backPane.getChildren().add(seat.getValue().betText);
        }
        backPane.getChildren().add(board);
        backPane.getChildren().add(potText);
        backPane.getChildren().add(messageText);
        backPane.getChildren().add(standUpButton);
        backPane.getChildren().add(actionButtons);
        backPane.getChildren().add(timer);

        /* Set the scene */
        Scene s = new Scene(backPane, startWidth, startHeight);
        setTitle("Table " + data.getTableID());
        setMinHeight(startHeight);
        setMinWidth(startWidth);
        setScene(s);
    }

    /**
     * Initialise the table and everything on it.
     */
    private void initTable() {
        /* Create the table */
        table = new Ellipse();
        table.setFill(Color.GREEN);
        table.radiusXProperty().bind(widthProperty().divide(3));
        table.radiusYProperty().bind(heightProperty().divide(4));
        table.centerXProperty().bind(widthProperty().divide(2));
        table.centerYProperty().bind(heightProperty().divide(2));

        /* Create the board */
        board = new BoardRegion();

        /* Create the text above and below the board */
        potText = new Text();
        potText.translateYProperty().bind(board.heightProperty().divide(2).add(15));
        messageText = new Text();
        messageText.translateYProperty().bind(
                board.heightProperty().divide(2).negate().subtract(15));
    }

    /**
     * Initialise the seats.
     */
    private void initSeats() {
        /* Create the seats */
        seats = HashBiMap.create();
        for (int j = 0; j < data.getMaxHands(); j++) {
            /* Place a new seat in each position */
            seats.put(j, new TableSection(j));
            if (data.seatIsOccupied(j)) {
                /* If the seat is already occupied, do so */
                seats.get(j).setIsFree(false, false);
            }
        }
    }

    /**
     * Initialise the buttons. This includes the all of the action buttons, and
     * the raise slider and stand up button.
     */
    private void initButtons() {

        /* Create the stand up button and place it somewhere at the bottom right */
        standUpButton = new Button("Stand up");
        standUpButton.translateXProperty().bind(widthProperty().divide(3));
        standUpButton.translateYProperty().bind(
                heightProperty().divide(2)
                        .subtract(standUpButton.heightProperty()));

        /* Send the standup command when it is clicked */
        standUpButton.setOnMouseClicked((e) -> {
            if (!(playerSeat == null)) {
                sendCommand("standup:" + playerSeat.getSeatID());
            }
        });

        /* Only enable the stand up button when we the player is sat down */
        standUpButton.setDisable(true);

        actionButtons = new HBox();
        /* Create the first 3 action buttons, each taking up 1/12 of the width of
         * the stage
         */
        Button foldBtn, checkBtn, callBtn, raiseBtn;
        foldBtn = new Button("Fold");
        foldBtn.maxWidthProperty().bind(widthProperty().divide(12));
        foldBtn.minWidthProperty().bind(widthProperty().divide(12));

        checkBtn = new Button("Check");
        checkBtn.maxWidthProperty().bind(widthProperty().divide(12));
        checkBtn.minWidthProperty().bind(widthProperty().divide(12));

        callBtn = new Button("Call\n100");
        callBtn.maxWidthProperty().bind(widthProperty().divide(12));
        callBtn.minWidthProperty().bind(widthProperty().divide(12));

        /* Create the raise slider with some deafult minima and maxima,
         * taking up 1/4 of the width of the stage.
         * TODO the minima and maxima should depend upon blind sizes, in data.
         */
        Slider raiseSlider = new Slider();
        raiseSlider.setValue(200);
        raiseSlider.setMin(200);
        raiseSlider.setMax(1000);
        raiseSlider.setBlockIncrement(200);
        raiseSlider.maxWidthProperty().bind(widthProperty().divide(4));
        raiseSlider.minWidthProperty().bind(widthProperty().divide(4));
        raiseSlider.setMajorTickUnit(200);
        raiseSlider.setShowTickMarks(true);

        /* Create the raise button, with text the same as the value of the slider,
         * and make sure it is the same width as the slider
         */
        raiseBtn = new Button();
        raiseBtn.textProperty().bind(raiseSlider.valueProperty().asString("%10.0f"));
        raiseBtn.maxWidthProperty().bind(raiseSlider.widthProperty());
        raiseBtn.minWidthProperty().bind(raiseSlider.widthProperty());

        /* Create a container for the raise slider and button */
        VBox raisePane = new VBox();
        raisePane.getChildren().add(raiseSlider);
        raisePane.getChildren().add(raiseBtn);
        raisePane.minHeightProperty().bind(
                raiseSlider.heightProperty().add(raiseBtn.heightProperty()));
        raisePane.maxHeightProperty().bind(
                raiseSlider.heightProperty().add(raiseBtn.heightProperty()));
        raisePane.minWidthProperty().bind(raiseSlider.widthProperty());
        raisePane.maxWidthProperty().bind(raiseSlider.widthProperty());

        /* Make sure all of the buttons are the same height */
        checkBtn.minHeightProperty().bind(raisePane.heightProperty());
        checkBtn.maxHeightProperty().bind(raisePane.heightProperty());
        foldBtn.minHeightProperty().bind(raisePane.heightProperty());
        foldBtn.maxHeightProperty().bind(raisePane.heightProperty());
        callBtn.minHeightProperty().bind(raisePane.heightProperty());
        callBtn.maxHeightProperty().bind(raisePane.heightProperty());

        /* Add the appropriate events when each button is clicked on */
        foldBtn.setOnMouseClicked(e -> sendCommand("game:fold:" + playerSeat.getSeatID()));
        checkBtn.setOnMouseClicked(e -> sendCommand("game:check:" + playerSeat.getSeatID()));
        callBtn.setOnMouseClicked(e -> sendCommand("game:call:" + playerSeat.getSeatID()));
        raiseBtn.setOnMouseClicked(e -> sendCommand("game:raise:" + playerSeat.getSeatID() + ":" + (int) raiseSlider.getValue()));

        /* Add all of the buttons to the container */
        actionButtons.getChildren().add(foldBtn);
        actionButtons.getChildren().add(checkBtn);
        actionButtons.getChildren().add(callBtn);
        actionButtons.getChildren().add(raisePane);

        /* Make sure the container is flush with the contents */
        actionButtons.maxHeightProperty().bind(raisePane.heightProperty());
        actionButtons.minHeightProperty().bind(raisePane.heightProperty());
        actionButtons.minWidthProperty().bind(
                checkBtn.widthProperty()
                        .add(foldBtn.widthProperty())
                        .add(callBtn.widthProperty())
                        .add(raisePane.widthProperty()));
        actionButtons.maxWidthProperty().bind(
                checkBtn.widthProperty()
                        .add(foldBtn.widthProperty())
                        .add(callBtn.widthProperty())
                        .add(raisePane.widthProperty()));

        /* Put the container in the bottom left corner */
        actionButtons.translateXProperty().bind(
                widthProperty().divide(2).negate()
                        .add(actionButtons.widthProperty().divide(2)).add(10));
        actionButtons.translateYProperty().bind(
                heightProperty().divide(2)
                        .subtract(actionButtons.heightProperty().divide(2)).subtract(20));

        /* Make all of the buttons invisible to start with */
        actionButtons.getChildren().forEach(n -> n.setDisable(true));
        actionButtons.setVisible(false);

    }

    /**
     * Initialise the timer.
     */
    private void initTimer() {

        /* Create the timer bar, and make it the correct shape */
        timer = new ProgressBar();
        timer.setRotate(-90);
        timer.minWidthProperty().bind(seats.get(0).hr.heightProperty());
        timer.maxWidthProperty().bind(seats.get(0).hr.heightProperty());

        /* Create a new clock, and bind the progress bar to it */
        IntegerProperty timeSeconds = new SimpleIntegerProperty(TIMERLENGTH * 100);
        timeSeconds.set((TIMERLENGTH + 1) * 100);
        timer.progressProperty().bind(
                timeSeconds.divide(TIMERLENGTH * 100.0));

        /* Create a new timeline which counts down the clock */
        timeline = new Timeline();
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(TIMERLENGTH + 1),
                        new KeyValue(timeSeconds, 0)));

        /* Apply the correct colour to the progress bar when we reach 1/2 and 3/4 time */
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(timer.styleProperty(), "")));
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds((TIMERLENGTH + 1) / 2),
                        new KeyValue(timer.styleProperty(), "-fx-accent: orange")));
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(((TIMERLENGTH + 1) * 3) / 4),
                        new KeyValue(timer.styleProperty(), "-fx-accent: red")));

        timer.setVisible(false);
    }

    //<editor-fold defaultstate="collapsed" desc="Init">

    /**
     * Updates the state of the table based on new data. Namely, if anyone new
     * has sat down, or a game is in progress since the original data was obtained,
     * this will be shown.
     *
     * @param newData The new table data to update the table with.
     */
    private void updateData(PokerTableDataClient newData) {
        /* Make sure the IntegerProperty's are as they should be */
        newData.updateProperties();

        /* Go through all of the hands at the table. If any of them have been
         * either taken or vacated since the original data, update this.
         */
        for (Integer i = 0; i < data.getMaxHands(); i++) {
            if (data.seatIsOccupied(i) && !newData.seatIsOccupied(i)) {
                seats.get(i).setIsFree(true, true);
            } else if (!data.seatIsOccupied(i) && newData.seatIsOccupied(i)) {
                seats.get(i).setIsFree(false, false);
            }
        }

        /* Go through all of the handsat the table, and make sure the cards are
         * initialised. If a game is in progress, show the cards of those in the
         * hand.
         */
        for (Map.Entry<Integer, PlayerData> player :
                newData.getPlayerAndIndex().entrySet()) {
            player.getValue().resetHand();
            if (newData.getStageOfPlay() >= 1) {
                if (!(playerSeat == null)
                        && player.getKey().equals(playerSeat.getSeatID())) {
                    /* We shouldn't be getting new data when we are already in
                     * the hand.
                     */
                    System.err.println("This should never have triggered");
                    seats.get(player.getKey())
                            .showCardFronts(player.getValue().getHand());
                } else if (player.getValue().isInHand()) {
                    seats.get(player.getKey()).showCardBacks();
                }
            }
        }

        /* If there need to be any cards on the table, show them */
        if (newData.getStageOfPlay() >= 2) {
            board.flop(newData.getBoardCard(0),
                    newData.getBoardCard(1),
                    newData.getBoardCard(2));
        }
        if (newData.getStageOfPlay() >= 3) {
            board.turn(newData.getBoardCard(3));
        }
        if (newData.getStageOfPlay() >= 4) {
            board.river(newData.getBoardCard(4));
        }

        /* Finally update the data */
        data = newData;
    }

    /**
     * Sends a command to the server. This command will be prepended with the
     * table ID.
     *
     * @param str The command to send to the server.
     */
    void sendCommand(String str) {
        try {
            socket.write("ID:" + this.getID() + ":" + str);
        } catch (IOException e) {
            System.err.println("Exception occurred when trying to send command");
        }
    }

    /**
     * Close the current table. Tells the server that the table is closing, and
     * also, if the player is at the table, tells the server that the player
     * has stood up.
     */
    void quit() {
        if (!(playerSeat == null)) {
            sendCommand("standup:" + playerSeat.getSeatID());
        }
        sendCommand("quit");
    }

    /**
     * Process a command sent from the server.
     *
     * @param com The command String received from the server. This is in general
     *            made up of multiple arguments separated by colons, which is split up using
     *            Util.expandCommand().
     */
    void processCommand(String com) throws CommandFormatException {

        /* If we receive update command, the server is sending updated TableData */
        if (com.equals("update")) {
            try {
                /* Try to read the TableData, and if we do, perform the necessary
                 * UI updates with updateData();
                 */
                Object o = null;// = socket.read();
                if (!(o instanceof PokerTableData)) throw new ClassNotFoundException();
                PokerTableDataClient newData = new PokerTableDataClient((PokerTableData) o);
                Platform.runLater(() -> updateData(newData));
                throw new IOException();
            } catch (IOException | ClassNotFoundException e) {
                throw new CommandFormatException(data.getTableID(), false, com);
            }
            //return;
        }

        //<editor-fold defaultstate="collapsed" desc="All of the expandable commands">
        /* All other valid commands have multiple arguments, which we expand out
         * using Util.expandCommand
         */
        if (com.contains(":")) {
            List<String> commands = Util.expandCommand(com);

            //<editor-fold defaultstate="collapsed" desc="seattaken command">
            /* If a seat is taken, we also need to know which seat it is, which
             * is in the second string
             */
            if ((commands.get(0).equals("seattaken") || commands.get(0).equals("sit"))
                    && commands.size() == 2) {
                try {

                    /* Get the seat number; if the command is NaN or too large
                     * or negative, a NumberFormatException is thrown.
                     */
                    Integer index = Integer.parseInt(commands.get(1));
                    if (index >= data.getMaxHands() || index < 0)
                        throw new NumberFormatException();

                    /* After getting a seattaken command, we receive the
                     * PlayerData for the player at the seat. If this isn't what
                     * we receive next, throw a ClassNotFoundException.
                     */
                    Object o = null;// = socket.read();
                    if (!(o instanceof PlayerData))
                        throw new ClassNotFoundException();

                    /* Once we have the PlayerData, we initialise the volatile
                     * fields, and put the player on the table
                     */
                    Platform.runLater(() -> {

                        PlayerData pd = (PlayerData) o;
                        pd.resetHand();
                        data.setSeatOccupied(index, pd);
                        /* if "sit" was received, the local player sits ... */
                        if (commands.get(0).equals("sit"))
                            takeSeat(index);
                            /* otherwise it is a remote player */
                        else {
                            seats.get(index).setIsFree(false, false);
                            seats.get(index).chipCountText
                                    .setText(pd.chipCount.toString());
                            //TODO possible set chipCount as IntegerProperty
                        }
                    });
                    throw new IOException();
                } catch (ClassNotFoundException | IOException e) {
                    System.err.println("Exception when getting new sitter");
                    throw new CommandFormatException(data.getTableID(), false,
                            String.join(":", commands));
                } catch (NumberFormatException e) {
                    System.err.println("The seat index was not right");
                    throw new CommandFormatException(data.getTableID(), false,
                            String.join(":", commands));
                }
                //return;
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="seatvacted command">
            /* If the primary command is "seatvacated", we must also be given
             * the seat number
             */
            if (commands.get(0).equals("seatvacated") && commands.size() == 2) {
                Platform.runLater(() -> {
                    try {

                        /* Try to get the seat number; if the second element of
                         * the command list is not an integer, or if it is too
                         * large an integer, a NumberFormatException is thrown
                         */
                        Integer index = Integer.parseInt(commands.get(1));
                        if (index >= data.getMaxHands() || index < 0)
                            throw new NumberFormatException();

                        /* If the seat being vacated is out seat, remove our
                         * seat, and allow us to sit in any of the other
                         * unoccupied seats
                         */
                        if (!(playerSeat == null)
                                && index.equals(playerSeat.getSeatID())) {

                            playerSeat = null;

                            seats.entrySet().stream()
                                    .filter((ts) -> ts.getValue().isFree)
                                    .forEachOrdered((ts) ->
                                            ts.getValue().setIsFree(true, true)
                                    );

                            standUpButton.setDisable(true);
                            actionButtons.setVisible(false);
                        }

                        /* Finally, set the previously occupied seat to be free,
                         * and remove all of its assets
                         */
                        data.setSeatFree(index);
                        seats.get(index).setIsFree(true, playerSeat == null);
                        seats.get(index).chipCountText.setText("");
                        seats.get(index).resetCards();
                        /* TODO if we bind the chipcount text to an integer
                         * property we may not need to do this.
                         * also should probably be removing cards if they are
                         * playing
                         */
                    } catch (NumberFormatException e) {
                        /* If we don't have a valid seat number to vacate, we
                         * needn't do anything.
                         * TODO throw a CommandFormatException
                         */
                        System.err.println("The seat index was not correct");

                    }
                });
                return;
            }
            //</editor-fold>

            /* Send any commands specific to the game to its own method */
            if (commands.get(0).equals("game")) {
                Platform.runLater(() -> {
                    try {
                        processGameCommand(commands);
                    } catch (CommandFormatException ex) {
                        System.err.println(String.join(":", commands));
                        System.err.println("TODO properly deal with this exception");
                    }
                });
            }
        }
//</editor-fold>

    }

    /**
     * Processes all commands from the server for game play. All such commands
     * commands should start with "game". The command forms are:
     * <ul>
     * <li>"game:start:..." and "game:end", both used to do clean up.</li>
     * <li>"game:start:dealer:$dealer:small:$small:big:$big", used to set the
     * positions of the dealer, small, and big blinds. </li>
     * <li>"game:rounddone:$pot", used to clean up after a round of betting,
     * and update the pot size. </li>
     * <li>"game:winner:$winner:$pot:...", used to indicate the winner of the
     * round, together with the amount they won, and optionally the
     * hand they won with. </li>
     * <li>"game:flop:...", "game:turn:$card", and "game:river:$card", used to
     * give the cards from the appropriate round. </li>
     * <li>"game:seat:$seatNo:...", used to pass information about a given
     * player, i.e. cards they have, betting action etc. </li>
     * </ul>
     *
     * @param commands The list of commands received from the server.
     */
    private void processGameCommand(List<String> commands)
            throws CommandFormatException {
        /* If the list contains the game command, strip it. It never should though */
        if (!commands.get(0).equals("game"))
            throw new CommandFormatException(data.getTableID(), false,
                    String.join(":", commands));

        commands.remove(0);

        //<editor-fold defaultstate="collapsed" desc="start and end commands">
        /* If the game is starting or ending, clean everything up
         * TODO we shouldn't have to do it at both start and end
         */
        if (commands.get(0).equals("start") ||
                commands.get(0).equals("end")) {

            /* Clean up all of the seats */
            for (Map.Entry<Integer, TableSection> seat : seats.entrySet()) {
                seat.getValue().resetCards();
                seat.getValue().betText.setText("");
                if (data.seatIsOccupied(seat.getKey()))
                    data.playerOnSeat(seat.getKey()).currentBet = 0;
            }

            /* Clean up the board */
            board.resetBoard();
            data.resetBoard();
            data.setPot(0);
            messageText.setText("");
            //potText.setText("");
            potText.textProperty().bind(data.potProperty().asString());

            /* Disable the action buttons unil they can be used again */
            actionButtons.getChildren().forEach((n) -> n.setDisable(true));
        }
        //</editor-fold>

        if (commands.size() == 1) return;

        //<editor-fold defaultstate="collapsed" desc="all multi argument commands">

        //<editor-fold defaultstate="collapsed" desc="start command">
        /* Handle the rest of the game start command. The command should be of
         * the form start:dealer:$dealer:small:$small:big:$big
         */
        if (commands.get(0).equals("start")) {

            /* Throw an exception if we don't get what we expect */
            if (!commands.get(1).equals("dealer") || !commands.get(3).equals("small")
                    || !commands.get(5).equals("big")) {
                throw new CommandFormatException(data.getTableID(), true,
                        String.join(":", commands));
            }
            /* Get the dealer, small blind, and big blind positions */
            Integer dealer, small, big;
            try {
                dealer = Integer.parseInt(commands.get(2));
                small = Integer.parseInt(commands.get(4));
                big = Integer.parseInt(commands.get(6));
                /* If any of them are NaN, or an invalid position, throw and
                 * exception.
                 */
                if ((dealer >= data.getMaxHands() || dealer < 0) ||
                        (small >= data.getMaxHands() || small < 0 || small.equals(dealer)) ||
                        (big >= data.getMaxHands() || big < 0 || big.equals(small))) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                throw new CommandFormatException(data.getTableID(), true,
                        String.join(":", commands));
            }

            /* Set the new positions in the data */
            data.setDealer(dealer);
            data.setSmallBlind(small);
            data.setBigBlind(big);

            /* Place the bets from the small and big blind
             * TODO the bet sizes should come from information in the data.
             */
            placeBet(data.getSmallBlind(), 100);
            placeBet(data.getBigBlind(), 200);

            return;
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="rounddone command">
        /* Finish up the current betting round */
        if (commands.get(0).equals("rounddone")) {
            /* The only argument we receive should be the pot size at the end of
             * the round
             */
            try {
                Integer newPot = Integer.parseInt(commands.get(1));
                if (newPot < 0)
                    throw new NumberFormatException();
                data.setPot(newPot);
            } catch (NumberFormatException ex) {
                /* If the pot was NaN (or negative), we can still cope, by adding
                 * together the bets from all players, which certainly should be
                 * correct.
                 */
                Logger.getLogger(PokerTableStage.class.getName()).log(Level.INFO, null, ex);
                if (data.getPot() == null)
                    data.setPot(0);
                for (PlayerData player : data.getPlayers()) {
                    if (player.isInHand() && player.currentBet != null)
                        data.addToPot(player.currentBet);
                }
            }
            //potText.setText(data.getPot().toString());

            for (Map.Entry<Integer, PlayerData> player :
                    data.getPlayerAndIndex().entrySet()) {
                seats.get(player.getKey()).betText.setText("");
                player.getValue().currentBet = 0;
            }

            resetTimer();
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="winner command">
        /* Handle the game winner command. Could be of two different forms:
         * 1)winner:$winner:$pot:$winningHand, if at showdown, or,
         * 2)winner:$winner:$pot, if from folding
         * TODO in general there could be multiple winners. this should just be
         *  doable by sending multiple winner commands from the server
         */
        if (commands.get(0).equals("winner")) {

            resetTimer();
            Integer winner, pot;
            String winningHand = null;
            switch (commands.size()) {
                /* If the winner came from showdown, we need to get the extra
                 * argument
                 */
                case 4:
                    winningHand = commands.get(3);
                    /* Get the first three arguments in both cases */
                case 3:
                    try {
                        winner = Integer.parseInt(commands.get(1));
                    } catch (NumberFormatException ex) {
                        /* If we don't know the winner, throw an exception */
                        throw new CommandFormatException(data.getTableID(),
                                true, String.join(":", commands));
                    }
                    try {
                        pot = Integer.parseInt(commands.get(2));
                    } catch (NumberFormatException ex) {
                        /* If we don't know the pot, get it from our data
                         * TODO though of course this will be wrong if there
                         *  are multiple winners
                         */
                        pot = data.getPot();
                    }
                    break;
                /* If we don't have the right number of arguments, throw an exception */
                default:
                    throw new CommandFormatException(data.getTableID(), true,
                            String.join(":", commands));
            }

            /* Display the winner, and update the chip counts */
            messageText.setText("Player " + winner + " wins");
            if (data.seatIsOccupied(winner)) {
                data.playerOnSeat(winner).chipCount += pot;
                seats.get(winner).chipCountText
                        .setText(data.playerOnSeat(winner).chipCount.toString());
            }
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="flop, turn, river">
        /* Handle cards for the board. These should be the round name, followed
         * by either 3 or 1 card as a string
         */
        if (commands.get(0).equals("flop")) {
            /* If we don't have enough cards, throw an exception */
            if (commands.size() < 4)
                throw new CommandFormatException(data.getTableID(),
                        true, String.join(":", commands));
            data.setBoardCard(new Card(commands.get(1)), 0);
            data.setBoardCard(new Card(commands.get(2)), 1);
            data.setBoardCard(new Card(commands.get(3)), 2);
            board.flop(data.getBoardCard(0), data.getBoardCard(1), data.getBoardCard(2));
            return;
        }

        if (commands.get(0).equals("turn")) {
            if (commands.size() < 2)
                throw new CommandFormatException(data.getTableID(),
                        true, String.join(":", commands));
            data.setBoardCard(new Card(commands.get(1)), 3);
            board.turn(data.getBoardCard(3));
            return;
        }
        if (commands.get(0).equals("river")) {
            if (commands.size() < 2)
                throw new CommandFormatException(data.getTableID(),
                        true, String.join(":", commands));
            data.setBoardCard(new Card(commands.get(1)), 4);
            board.river(data.getBoardCard(4));
            return;
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="information on player">
        /* Handle a command for a given seat. There are 4 different forms
         * 1a) seat:$seatNo:action:$action, giving the action the player performed,
         * 1b) seat:$seatNo:action:$action:$actionCost, if the player either
         * called or raised, where $actionCost is the amount of that,
         * 2) seat:$seatNo:toact:$callCost, giving the player to act, and their
         * cost to call, and
         * 3) seat:$seatNo:card:$cardIndex:$card, giving the card of the player (
         * which in most cases will be empty).
         */
        if (commands.get(0).equals("seat")) {
            /* All of the seat commands must have at least 4 arguments */
            if (commands.size() < 4) {
                throw new CommandFormatException(data.getTableID(), true,
                        String.join(":", commands));
            }

            /* Try to get the seat index */
            Integer seatIndex;
            try {
                seatIndex = Integer.parseInt(commands.get(1));
                if (seatIndex < 0 || seatIndex >= data.getMaxHands())
                    throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                /* If the index is NaN, or not a valid seat index, throw an exception */
                throw new CommandFormatException(data.getTableID(), true,
                        String.join(":", commands));
            }

            if (commands.get(2).equals("action")) {
                /* If we received an action command, set this on the table, and
                 * on the player
                 */
                messageText.setText("Player " + seatIndex + " did " + commands.get(3));
                try {
                    data.playerOnSeat(seatIndex).setAction(
                            Util.PokerAction.valueOf(commands.get(3)));
                } catch (IllegalArgumentException ex) {
                    /* If we can't set the action in the data, it isn't a problem */
                    System.err.println("Could not convert action string to "
                            + "action");
                }

                /* If the action was a call or a raise, place the bet */
                if (commands.get(3).equals("RAISE") ||
                        commands.get(3).equals("CALL")) {
                    /* If we haven't received the bet size, throw an exception */
                    if (commands.size() < 5)
                        throw new CommandFormatException(data.getTableID(), true,
                                String.join(":", commands));

                    /* Try to get the bet size */
                    Integer bet;
                    try {
                        bet = Integer.parseInt(commands.get(4));
                    } catch (NumberFormatException ex) {
                        /* If the bet size is NaN, throw an exception */
                        throw new CommandFormatException(data.getTableID(), true,
                                String.join(":", commands));
                    }
                    /* Place the bet graphically and in the data */
                    placeBet(seatIndex, bet);
                }

                if (commands.get(3).equals("FOLD")) {
                    seats.get(seatIndex).resetCards();
                }
                /* Once the player has acted, we can stop their timer */
                resetTimer();
            }

            if (commands.get(2).equals("toact")) {
                /* Try to get the cost for the player to act */
                Integer toCall;
                try {
                    toCall = Integer.parseInt(commands.get(3));
                } catch (NumberFormatException ex) {
                    throw new CommandFormatException(data.getTableID(), true,
                            String.join(":", commands));
                }

                /* If the player to act is the local player, make it possible for
                 * them to act.
                 */
                if (!(playerSeat == null) && seatIndex.equals(playerSeat.getSeatID())) {

                    if (toCall.equals(0)) {
                        /* If the toCall price is 0, we can check, but not call */
                        actionButtons.getChildren().get(1).setDisable(false);   //check button
                        actionButtons.getChildren().get(2).setDisable(true);    //call button
                    } else {
                        /* Otherwise we can call but not check */
                        actionButtons.getChildren().get(1).setDisable(true);
                        actionButtons.getChildren().get(2).setDisable(false);
                        ((Button) actionButtons.getChildren().get(2))
                                .setText("Call\n" + toCall.toString());

                    }

                    /* The raise and fold options are always available
                     * TODO the raise options shouldn't be available if we have
                     * fewer chips than the price to call
                     */
                    actionButtons.getChildren().get(3).setDisable(false);
                    ((Slider) ((VBox) actionButtons.getChildren().get(3))
                            .getChildren().get(0)).setMin(200);
                    actionButtons.getChildren().get(0).setDisable(false);

                    /* Get the attention of the local player */
                    requestFocus();
                } else {
                    /* If the player to act is not the current player, disable
                     * their actions.
                     * TODO allow action out of turn, to be stored on the server
                     */
                    actionButtons.getChildren().forEach((n) ->
                            n.setDisable(true)
                    );
                }

                /* Start the timer running for the player to act */
                runTimer(seatIndex);

                return;
            }


            if (commands.get(2).equals("card")) {

                if (commands.size() < 5)
                    throw new CommandFormatException(data.getTableID(), true,
                            String.join(":", commands));

                /* Try to the get the index of the card */
                int cardIndex;
                try {
                    cardIndex = Integer.parseInt(commands.get(3));
                } catch (NumberFormatException ex) {
                    throw new CommandFormatException(data.getTableID(), true,
                            String.join(":", commands));
                }

                /* Set the card in the hand */
                if (cardIndex == 0) {
                    data.playerOnSeat(seatIndex).setHand(
                            new Card(commands.get(4)),
                            data.playerOnSeat(seatIndex).getHand().getCard(1));
                } else if (cardIndex == 1) {
                    data.playerOnSeat(seatIndex).setHand(
                            data.playerOnSeat(seatIndex).getHand().getCard(0),
                            new Card(commands.get(4)));

                    /* Show the cards in the the hands, either front up, if the
                     * players seat, or front down otherwise
                     */
                    if (seats.get(seatIndex).equals(playerSeat)) {
                        seats.get(seatIndex).showCardFronts(
                                data.playerOnSeat(seatIndex).getHand());
                    } else {
                        seats.get(seatIndex).showCardBacks();
                    }

                }
            }
            //return;
        }
        //</editor-fold>

        //</editor-fold>
    }
    //</editor-fold>

    /**
     * Updates the graphics to show the local player at the required seat.
     * Namely, removes the ability to sit at other seats and enables the action
     * buttons.
     *
     * @param index The seat index that the local player is sat at.
     */
    private void takeSeat(Integer index) {

        seats.forEach((seatIndex, seat) -> seat.setIsFree(seat.isFree, false));

        /* Set the player seat as the correct one */
        playerSeat = seats.get(index);
        playerSeat.setIsFree(false, false);
        playerSeat.chipCountText.setText(data.playerOnSeat(index).chipCount.toString());

        /* Show the standup button and the action buttons */
        standUpButton.setDisable(false);
        actionButtons.setVisible(true);
    }

    /**
     * Place a bet from a player.
     *
     * @param index The seat index of the player to place a bet
     * @param bet   The amount that the bet is worth
     */
    private void placeBet(Integer index, Integer bet) {

        /* This should never be triggered: it should be verified that the player
         * has enough chips to place a bet on the server.
         */
        if (bet > data.playerOnSeat(index).chipCount) {
            bet = data.playerOnSeat(index).chipCount;
        }

        /* Take off the bet from the players chip count; if the player had
         * already placed a bet, refund the old bet.
         */
        data.playerOnSeat(index).chipCount -= bet;
        if (data.playerOnSeat(index).currentBet != null)
            data.playerOnSeat(index).chipCount +=
                    data.playerOnSeat(index).currentBet;

        data.playerOnSeat(index).currentBet = bet;

        /* Update the UI to display the new bet amount and total chip count
         * TODO this could be removed by binding the text to an appropriate
         * IntegerProperty for the chipCount and currentBet
         */
        seats.get(index).betText.setText(bet.toString());
        seats.get(index).chipCountText
                .setText(data.playerOnSeat(index).chipCount.toString());

    }

    /**
     * Resets the countdown timer.
     */
    private void resetTimer() {
        /* Make sure the timeline isn't running */
        if (timeline != null) {
            timeline.stop();
        }

        /* Reset the clock */
        IntegerProperty timeSeconds = new SimpleIntegerProperty(TIMERLENGTH * 100);

        /* Rebind the progress bar to the new clock */
        timer.progressProperty().bind(
                timeSeconds.divide(TIMERLENGTH * 100.0));

        /* Rebind the end of the timeline to the new clock */
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(TIMERLENGTH + 1),
                        new KeyValue(timeSeconds, 0)));

        /* Hide it */
        timer.setVisible(false);
    }

    /**
     * Starts the countdown timer. Also changes the location to be next to the
     * correct player.
     *
     * @param index The player for whom the countdown is for.
     */
    private void runTimer(Integer index) {

        /* Make sure the timer starts from the beginning */
        resetTimer();

        /* Send a fold command to the server when the timer finishes, so long
         * as it is the local players turn
         */
        timeline.setOnFinished((eh) -> {
            //TODO send the command to the server that the player folded
        });

        /* Move the y coordinate to be same as that of the hand */
        timer.translateYProperty().bind(
                seats.get(index).hr.translateYProperty());

        /* Move the x coordinate to be close to that of the hand */
        if (index < (data.getMaxHands()) / 2.0f)
            /* If the hand is on the left half of the table, the timer goes to
             * the left of the hand...
             */
            timer.translateXProperty().bind(
                    seats.get(index).hr.translateXProperty()
                            .add(seats.get(index).hr.widthProperty().divide(2))
                            .add(timer.heightProperty().divide(2)));
        else
            /* ...and vice versa */
            timer.translateXProperty().bind(
                    seats.get(index).hr.translateXProperty()
                            .subtract(seats.get(index).hr.widthProperty().divide(2))
                            .subtract(timer.heightProperty().divide(2)));

        /* Start the timer */
        timer.setVisible(true);
        timeline.playFromStart();
    }

    /**
     * Get the table ID from the data.
     *
     * @return The table ID from the table data, or -1 if we have no table data
     */
    int getID() {
        if (data != null)
            return data.getTableID();
        else
            return -1;
    }

    /**
     * Two PokerTableStages are equal iff their id is the same.
     *
     * @param o the reference object with which to compare.
     * @return true if this table is the same as the obj argument; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PokerTableStage)) {
            return false;
        }
        PokerTableStage table2 = (PokerTableStage) o;
        return getID() == table2.getID();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + getID();
        return hash;
    }

    //<editor-fold defaultstate="collapsed" desc="Timer manipulation">

    /**
     * Wrapper class for the five community cards.
     */
    private class BoardRegion extends HBox {

        List<CardRegion> cardRegions;

        /**
         * Create the five community cards.
         */
        BoardRegion() {
            int spacing = 3;
            int cardWidth = 50;
            int cardHeight = 80;

            setMaxSize(5 * cardWidth + 5 * spacing, cardHeight);
            setAlignment(Pos.CENTER);
            setSpacing(spacing);

            /* Create the five cards of the correct dimensions, and add them
             * to the container
             */
            cardRegions = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                CardRegion c = new CardRegion(cardWidth, cardHeight);
                c.setVisible(false);
                getChildren().add(c);
                cardRegions.add(c);
            }
        }

        /**
         * Hide all of the cards.
         */
        void resetBoard() {
            setVisible(false);
            cardRegions.forEach((c) -> {
                c.showBack();
                c.setVisible(false);
            });
        }

        /**
         * Show the first three cards. Being the cards provided.
         *
         * @param c1 1st Card
         * @param c2 2nd Card
         * @param c3 3rd Card
         */
        void flop(Card c1, Card c2, Card c3) {
            cardRegions.get(0).showFront(c1);
            cardRegions.get(0).setVisible(true);
            cardRegions.get(1).showFront(c2);
            cardRegions.get(1).setVisible(true);
            cardRegions.get(2).showFront(c3);
            cardRegions.get(2).setVisible(true);
            setVisible(true);
        }

        /**
         * Show the fourth card. Being the new card given.
         *
         * @param c 4th Card
         */
        void turn(Card c) {
            cardRegions.get(3).showFront(c);
            cardRegions.get(3).setVisible(true);
        }

        /**
         * Show the fifth card. Being the new card given.
         *
         * @param c 5th Card
         */
        void river(Card c) {
            cardRegions.get(4).showFront(c);
            cardRegions.get(4).setVisible(true);
        }
    }

    /**
     * Wrapper class for all of the player components. That is, the hand, the seat,
     * and the chip counts.
     */
    private class TableSection {
        private final SeatRegion sr;
        private final HandRegion hr;
        private final Text chipCountText;
        private final Text betText;
        private final int seatID;
        private boolean isFree;

        /**
         * Create the section at the specified index around the table. The index
         * increases anti-clockwise, starting from 0 at 6 o'clock.
         *
         * @param index The index of the seat to create
         */
        TableSection(int index) {
            seatID = index;
            isFree = true;

            /* The angle around the table is related to the index as such */
            int angle = (index * 360) / data.getMaxHands();

            sr = new SeatRegion(angle);
            /* Request the server to sit in the appropriate place when the sit
             * button is clicked.
             */
            sr.btn.setOnMouseClicked(e -> sendCommand("sit:" + getSeatID()));

            hr = new HandRegion(angle);

            chipCountText = new Text();
            chipCountText.setFill(Color.GOLD);
            /* Move the chip count text to appropriate place */
            chipCountText.translateXProperty().bind(hr.translateXProperty());
            if (seatID < data.getMaxHands() / 4.0 ||
                    (data.getMaxHands() - seatID) < data.getMaxHands() / 4.0) {
                /* If the seat is in the bottom half of the table, send the
                 * chip count text to the bottom of the hand.
                 */
                chipCountText.translateYProperty().bind(
                        hr.translateYProperty()
                                .add(hr.heightProperty().divide(2)).add(20));
            } else {
                /* Otherwise send it to the top */
                chipCountText.translateYProperty().bind(
                        hr.translateYProperty()
                                .subtract(hr.heightProperty().divide(2)).subtract(20));
            }

            betText = new Text();
            /* Send the bet text to be slightly inside the table from the appropriate
             * seat (divide(1.5)).
             */
            betText.translateXProperty().bind(
                    table.radiusXProperty().divide(1.5)
                            .multiply(Math.sin((angle * 2 * Math.PI) / 360)));
            betText.translateYProperty().bind(
                    table.radiusYProperty().divide(1.5)
                            .multiply(Math.cos((angle * 2 * Math.PI) / 360)));
        }


        int getStageID() {
            return getID();
        }

        int getSeatID() {
            return seatID;
        }

        /**
         * Set the seat as free or not. Moreover, if the seat is free, it is sittable
         * if the local player is not sitting anywhere else.
         *
         * @param isFree     true if the seat is free, false otherwise
         * @param isSittable true if the user can sit here, false otherwise
         */
        void setIsFree(boolean isFree, boolean isSittable) {
            this.isFree = isFree;

            if (isFree) {
                sr.setColor(Color.BLACK);
            } else {
                sr.setColor(Color.RED);
            }

            if (isSittable && playerSeat == null) {
                sr.enableButton();
            } else {
                sr.disableButton();
            }
        }

        void resetCards() {
            hr.showCardBacks();
            hideCards();
        }

        void hideCards() {
            hr.setCardsInvisible();
        }

        void showCards() {
            hr.setCardsVisible();
        }

        void showCardBacks() {
            hr.showCardBacks();
            showCards();
        }

        void showCardFronts(Cards hand) {
            hr.showCardFronts(hand);
            showCards();
        }

        /**
         * Two table sections are the same iff they belong to the same stage,
         * and their index is the same.
         *
         * @param o the reference object with which to compare.
         * @return true if this table section is the same as the obj argument; false otherwise.
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TableSection)) return false;
            TableSection ts2 = (TableSection) o;
            if (!(ts2.getStageID() == getStageID())) return false;
            return ts2.getSeatID() == getSeatID();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 61 * hash + this.getSeatID();
            hash = 61 * hash + this.getStageID();
            return hash;
        }

    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="equality methods">

    /**
     * Wrapper class for the seat and sit button.
     */
    private class SeatRegion extends StackPane {
        private final Rectangle r;
        private final Button btn;

        /**
         * Create the seat at the appropriate angle around the table.
         *
         * @param angle The angle around the table the seat should be positioned.
         */
        SeatRegion(int angle) {

            /* Create the seat and button. Add them to the container */
            r = new Rectangle(50, 50, Color.BLACK);
            btn = new Button("Sit Here");

            setMaxSize(150, 50);
            setAlignment(Pos.CENTER);
            getChildren().add(r);
            getChildren().add(btn);

            /* Rotate the seat so it is perpendicular to the table */
            if (angle % 90 == 0) {
                r.setRotate(angle);
            } else {
                /* This is rougly the correct angle. If the stage is resized,
                 * it will be slightly off.
                 */
                r.setRotate(Math.atan((800f / 500) * Math.cos(angle * Math.PI / 180)
                        / Math.sin(angle * Math.PI / 180))
                        * (360 / (2 * Math.PI)));
            }

            /* Move the seat to the correct angle around the table, slightly
             * outside the table.
             */
            translateXProperty().bind(
                    (table.radiusXProperty().add(50))
                            .multiply(Math.sin((angle * 2 * Math.PI) / 360)));
            translateYProperty().bind(
                    (table.radiusYProperty().add(50))
                            .multiply(Math.cos((angle * 2 * Math.PI) / 360)));

        }

        void setColor(Paint p) {
            r.setFill(p);
        }

        void disableButton() {
            getChildren().remove(btn);
        }

        void enableButton() {
            if (!getChildren().contains(btn))
                getChildren().add(btn);
        }

    }

    /**
     * Wrapper class for the two hole cards.
     */
    private class HandRegion extends HBox {

        private final CardRegion c1, c2;

        /**
         * Create two cards at the location specified by angle.
         *
         * @param angle The angle around the table that the hands should be placed;
         *              the angles start at 0 at the middle bottom (6 o'clock).
         */
        HandRegion(int angle) {

            int spacing = 3;
            int cardWidth = 50;
            int cardHeight = 80;

            /* Create the cards and show their back */
            c1 = new CardRegion(cardWidth, cardHeight);
            c2 = new CardRegion(cardWidth, cardHeight);
            c1.showBack();
            c2.showBack();

            /* Add the cards to the container */
            setMaxSize(2 * cardWidth + spacing, cardHeight);
            setAlignment(Pos.CENTER);
            setSpacing(5);
            setVisible(false);
            getChildren().add(c1);
            getChildren().add(c2);

            /* Place the hand at the correct angle around the table, slightly
             * outside the table (multiply(1.1))
             */
            translateXProperty().bind(
                    table.radiusXProperty().multiply(1.1)
                            .multiply(Math.sin((angle * 2 * Math.PI) / 360)));
            translateYProperty().bind(
                    table.radiusYProperty().multiply(1.1)
                            .multiply(Math.cos((angle * 2 * Math.PI) / 360)));


        }

        void setCardsVisible() {
            c1.setVisible(true);
            c2.setVisible(true);
            setVisible(true);
        }

        void setCardsInvisible() {
            c1.setVisible(false);
            c2.setVisible(false);
            setVisible(false);
        }

        void showCardFronts(Cards cards) {
            /* Make sure that we do have 2 cards */
            if (cards.getSize() < 1)
                cards.setCard(0, Card.EMPTY_CARD);
            if (cards.getSize() < 2)
                cards.setCard(1, Card.EMPTY_CARD);

            c1.showFront(cards.getCard(0));
            c2.showFront(cards.getCard(1));

        }

        void showCardBacks() {
            c1.showBack();
            c2.showBack();
        }
    }

    /**
     * Graphical representation of a card. Namely, it contains a rectangle, with
     * text on it.
     */
    private class CardRegion extends StackPane {
        private final Rectangle r;
        private final Text t;

        /**
         * Create a new empty card with the given width and height.
         *
         * @param width  The width of the card.
         * @param height The height of the card.
         */
        CardRegion(int width, int height) {
            /* Create the back of the card with the specified dimensions */
            r = new Rectangle();
            r.setWidth(width);
            r.setHeight(height);
            r.setArcHeight(10);
            r.setArcWidth(10);
            r.setStrokeWidth(2);
            r.setVisible(true);
            setMaxSize(width, height);

            /* Position the text on the card */
            t = new Text();
            t.setX(-10);
            t.setY(-10);
            t.setVisible(true);

            getChildren().add(r);
            getChildren().add(t);

            setVisible(false);
        }

        /**
         * Show the reverse of the card. That is, simply a rectangle with no text.
         */
        void showBack() {
            r.setFill(Color.RED);
            r.setStroke(Color.BLACK);
            t.setText("");

        }

        /**
         * Show the front of the card.
         *
         * @param c The card to display.
         */
        void showFront(Card c) {

            /* Set a white background */
            r.setFill(Color.WHITE);
            r.setStroke(Color.WHITE);

            /*If the card is a real card, display it */
            if (!c.equals(Card.EMPTY_CARD)) {
                t.setText(c.toString());
                t.setFont(new Font(20));
                /* Write the text in an appropriate colour for the suit */
                switch (c.getSuit()) {
                    case SPADE:
                        t.setFill(Color.DARKBLUE);
                        break;
                    case HEART:
                        t.setFill(Color.ORANGERED);
                        break;
                    case DIAMOND:
                        t.setFill(Color.DARKORANGE);
                        break;
                    case CLUB:
                        t.setFill(Color.DARKGREEN);
                        break;
                    default:
                        t.setFill(Color.BLACK);
                }
            }
        }
    }
    //</editor-fold>
}
