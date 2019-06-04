package space.poulter.poker.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import space.poulter.poker.CommandFormatException;
import space.poulter.poker.PokerPacket;
import space.poulter.poker.PokerSocket;
import space.poulter.poker.PokerTableData;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Em Poulter
 */
public class OldPokerClient extends Application {

    private static final Integer DEFAULT_PORT = 1111;
    private final Object socketSyncLock = new Object();
    private Stage primaryView;
    private Map<Integer, PokerTableStage> windows = new HashMap<>();
    private ClientSideSocket socket = null;
    private Map<Integer, PokerTableDataClient> tablesData = new HashMap<>();

    /**
     * Launch the application if not run as JavaFX
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initialises the main stage of the application. Sets the close behaviour, and adds a menu, to enable
     * connecting to a server.
     *
     * @param primaryStage The main stage of the application.
     */
    private void init(@NotNull Stage primaryStage) {

        /* Set the primary stage.
         * TODO why do we actually need this? */
        this.primaryView = primaryStage;

        /* Set the behaviour of the application when the primary stage is closed */
        primaryStage.setOnCloseRequest(event -> {

            if (windows.size() > 0) {
                /* If there are other windows open, alert the user */
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "You have other windows open. Still exit?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    /* If the user wants to close anyway, close all of the other open windows */
                    windows.forEach((tableID, table) -> table.quit());

                } else {
                    /* If the alert was cancelled, don't close the application. */
                    event.consume();
                }
            }

            /* When there are no other windows, close the socket and exit */
            if (socket != null) {
                try {
                    //socket.initiateClose();
                    synchronized (socketSyncLock) {
                        socketSyncLock.wait();
                        socket.close();
                    }
                    throw new IOException();
                } catch (IOException | InterruptedException ex) {
                    System.err.println("Exception occurred when closing socket.");
                }
            }

            Platform.exit();
        });

        /* Create the menu bar */
        MenuBar menuBar = new MenuBar();

        /* Create the File menu */
        Menu menuFile = new Menu("File");

        /* Two create options within file menu */
        MenuItem menuFileConnectToServer = new MenuItem("Connect to server");
        MenuItem menuFileExit = new MenuItem("Exit");

        /* Set the behaviour of the options */
        /* "Connect to server" brings up a server address dialog to connect to, tries to connect,
         * possibly sends an auth request, then requests the tables from the data */
        menuFileConnectToServer.setOnAction(e -> {
            /* Show the connect to server dialog */
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Connect to server");
            dialog.setHeaderText("Enter server address");
            dialog.setContentText("Server address:");

            /* Loop until we get a valid response from the user */
            Optional<String> result;
            do {
                /* Wait for the server address from the user */
                result = dialog.showAndWait();

                /* If the user enters an address, try to connect to the server*/
                if (result.isPresent() && !result.get().equals("")) {
                    String response = result.get();
                    try {

                        /* Get the port of the server */
                        Integer port = OldPokerClient.DEFAULT_PORT;
                        /* If the user response contains a colon, the part after should be the port,
                         * and the part before the address.
                         */
                        if (response.contains(":")) {
                            port = Integer.parseInt(response.substring(response.indexOf(":") + 1));
                            response = response.substring(0, response.indexOf(":"));
                        }

                        /* Try to connect to the server. */
                        InetAddress address = InetAddress.getByName(response);
                        Socket s = new Socket(address, port);
                        socket = new ClientSideSocket(s);

                        /* Start a thread to complete the connection to the server */
                        Thread t = new Thread(() -> {
                            try {
                                /* Wait for the connection to the server to complete. */
                                synchronized (socketSyncLock) {
                                    socketSyncLock.wait();
                                }
                                /* Request the table list from the server */
                                socket.write("Get Tables");
                            } catch (IOException | InterruptedException | IllegalMonitorStateException ex) {
                                System.err.println("Exception when waiting for connection to complete");
                                //TODO should handle this exception better
                            }
                        });
                        t.setDaemon(true);
                        t.start();

                        break;

                    } catch (NumberFormatException | UnknownHostException ex) {
                        /* If the address is not valid, alert the user, then show the dialog again */
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Error when connecting to server");
                        alert.setHeaderText("The address entered is not a valid address");
                        alert.showAndWait();
                        System.err.println("Couldn't convert to ip address");
                    } catch (IOException ex) {
                        /* If the connection to the server couldn't be completed, alert the user, then show
                         * the dialog again.
                         */
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Error when connecting to server");
                        alert.setHeaderText("Could not connect to the server");
                        alert.showAndWait();
                        System.err.println("Couldn't connect to socket");
                    }
                } else {
                    /* Otherwise, if the user entered an empty string or cancelled the dialog, we're done here. */
                    break;
                }
            } while (true);

        });
        /* "Exit" just closes the app, in the same way as defined above */
        menuFileExit.setOnAction(e -> primaryStage.close());

        /* Add all of the menu items together */
        menuFile.getItems().addAll(menuFileConnectToServer, menuFileExit);
        menuBar.getMenus().add(menuFile);

        /* Set the scene, and show it. */
        Scene scene = new Scene(new VBox(), 400, 350);
        ((VBox) scene.getRoot()).getChildren().addAll(menuBar);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Open a new window be opened with the given data. Creates the new table, sends a notification to the server
     * that the user is viewing the table, and adds the new window to the list of windows the user has open.
     *
     * @param dat The data for the table which the user wants to view.
     */
    private void requestView(@NotNull PokerTableDataClient dat) {
        /* If the requested window is already open, bring it to the fore, and return */
        if (windows.containsKey(dat.getTableID())) {
            windows.get(dat.getTableID()).requestFocus();
            return;
        }

        try {
            /* Send a message to the server to open the given table */
            socket.write("View:" + dat.getTableID());

            /* Create the new table */
            PokerTableStage newTable = new PokerTableStage(dat, socket);

            /* When the new table is closed, clean it up and remove it from the list of windows */
            newTable.setOnCloseRequest(e -> {
                newTable.quit();
                windows.remove(newTable.getID());
            });

            /* Add the new window to the list of windows */
            windows.put(dat.getTableID(), newTable);

            /* Request the server for updated info, and show the table */
            newTable.sendCommand("update");
            newTable.show();

        } catch (IOException e) {
            /* If there was a problem with server communication, just write something
             * TODO should be more robust here */
            System.err.println("Exception occurred when viewing new table");
            e.printStackTrace();
        }
    }

    /**
     * Create the list of available tables from the server. This is a table of each of the poker tables available on the
     * server, displaying the id and number of seats, and a button to allow the user to go to the table.
     *
     * @return The table of available poker tables, inside an accordion view.
     */
    private Accordion formatTableList() {

        /* Create the table view and list of poker tables to go in it. */
        TableView<PokerTableDataClient> table = new TableView<>();
        ObservableList<PokerTableDataClient> pokerTables = FXCollections.observableArrayList(tablesData.values());
        //TODO wouldn't it be better to pass the tablesData as an argument rather than being a global variable?
        table.setItems(pokerTables);

        /* Create three columns, the tableID, the number of seats at the table, and the button to join the table */
        TableColumn<PokerTableDataClient, Integer> tableIDCol = new TableColumn<>("Table ID");
        TableColumn<PokerTableDataClient, Integer> maxHandsCol = new TableColumn<>("Max hands");
        TableColumn<PokerTableDataClient, PokerTableDataClient> buttonCol = new TableColumn<>("");
        //TODO we will want more columns in the future.

        /* Set the properties to bind to in the data */
        tableIDCol.setCellValueFactory(t -> t.getValue().tableIDProperty().asObject());
        maxHandsCol.setCellValueFactory(t -> t.getValue().maxHandsProperty().asObject());
        buttonCol.setCellValueFactory(t -> new ReadOnlyObjectWrapper<>(t.getValue()));

        /* Create the button in the button column cells */
        buttonCol.setCellFactory(cell -> new TableCell<PokerTableDataClient, PokerTableDataClient>() {
            Button b = new Button("View");

            @Override
            public void updateItem(PokerTableDataClient dat, boolean empty) {
                /* Set the cell to contain the button */
                super.updateItem(dat, empty);
                setGraphic(b);
                /* Add a listener to open the table when the button is clicked */
                b.setOnMouseClicked(event -> requestView(dat));
            }
        });

        /* Add the columns to the table */
        table.getColumns().add(tableIDCol);
        table.getColumns().add(maxHandsCol);
        table.getColumns().add(buttonCol);

        /* Put the table in an accordion view, and expand it */
        TitledPane t1 = new TitledPane("Tables", table);
        Accordion accordion = new Accordion();
        accordion.getPanes().add(t1);
        accordion.setExpandedPane(t1);
        return accordion;
    }

    /**
     * Start the application.
     *
     * @param primaryStage The initial stage of the application.
     */
    @Override
    public void start(Stage primaryStage) {
        init(primaryStage);
    }

    class ClientSideSocket extends PokerSocket {

        ClientSideSocket(Socket s) throws IOException {
            super(s);
        }

        @Override
        public void processCommand(PokerPacket pak) {

        }

        /* TODO - completely rewrite the communication. It is far too ad hoc at the moment. Apply a method similar to
         *  e.g SSH, a variety of message codes etc.
         */
        public void processCommand(String str) {
            if (str.equals("auth:req")) {
                Platform.runLater(() -> {
                    LoginDialog login = new LoginDialog();
                    Optional<Pair<String, String>> result = login.showAndWait();
                    result.ifPresent(usernamePassword -> {
                        try {
                            socket.write("auth:user:" + usernamePassword.getKey() + ":pass:" + usernamePassword.getValue());
                        } catch (IOException ex) {
                            System.err.println("Exception when sending username and password");
                        }
                    });
                });
                return;
            }

            if (str.equals("auth:done")) {
                try {
                    synchronized (socketSyncLock) {
                        socketSyncLock.notify();
                    }
                } catch (IllegalMonitorStateException ex) {
                    System.err.println("Exception when connecting");
                }
            }

            if (str.startsWith("ID:")) {
                Integer index = Integer.parseInt(str.substring(3, str.indexOf(':', 3)));
                if (!windows.containsKey(index)) return;
                PokerTableStage table = windows.get(index);
                //System.out.println(str);
                try {
                    table.processCommand(str.substring(str.indexOf(':', 3) + 1));
                } catch (CommandFormatException ex) {
                    ex.printStackTrace();
                    //TODO do something better if we get a command format exception
                }
                return;
            }
            if (str.equals("Table List:")) {
                try {
                    //Object o = read();
                    Object o = null;
                    if (!(o instanceof Integer)) throw new ClassNotFoundException();
                    Integer noTables = (Integer) o;
                    //System.out.println(noTables);
                    for (int i = 0; i < noTables; i++) {
                        //o = read();
                        if (!(o instanceof PokerTableData)) throw new ClassNotFoundException();
                        PokerTableDataClient data = new PokerTableDataClient((PokerTableData) o);
                        data.updateProperties();
                        tablesData.put(data.getTableID(), data);
                        //System.out.println(data.getTableID());
                    }
                    throw new IOException();
                } catch (IOException | ClassNotFoundException ex) {
                    System.err.println("Exception occurred when receiving table list");
                    System.err.println(ex);
                }
                Platform.runLater(() ->
                        ((VBox) primaryView.getScene().getRoot()).getChildren().add(formatTableList())
                );
                return;
            }

            if (str.equals("Exit")) {
                synchronized (socketSyncLock) {
                    socketSyncLock.notify();
                }
            }
        }

    }

}
