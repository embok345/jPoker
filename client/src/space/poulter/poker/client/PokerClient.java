package space.poulter.poker.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.poulter.poker.PokerPacket;
import space.poulter.poker.PokerSocket;
import space.poulter.poker.Util;
import space.poulter.poker.codes.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PokerClient extends Application {

    /* The version of the client, to match the version of the server. */
    private static final String CLIENT_VERSION = Util.VERSION_STR + "_client" + "\r\n";
    /* Logger for the class. */
    private final Logger log = Logger.getLogger(getClass().getName());
    /* A mapping containing all of the tables which the client is connected to, indexed by the tableId. */
    private Map<Integer, ClientPokerTable> tables = new HashMap<>();
    /* The socket which is connected to the server. */
    //TODO we may be able to have multiple connections.
    private PokerClientSocket clientSocket;
    {
        log.setLevel(Util.LOG_LEVEL);
    }

    /**
     * Try to connect to the server with the given ip address and port. Sends the version string to the
     * server once it is connected, and if that is correct, returns the new connection encapsulated in PokerClientSocket.
     * If the client cannot connect to the server, or if the versions do not match, returns null.
     * @param ip The ip of the server to connect to.
     * @param port The port that the server is listening on.
     * @return The new Socket connected to the server, or null if the connection couldn't complete.
     */
    @Nullable
    private PokerClientSocket connectToServer(String ip, int port) {

        Socket newSocket;
        BufferedInputStream buffIn;
        BufferedOutputStream buffOut;

        try {
            /* Connect to the server. */
            newSocket = new Socket(ip, port);
            log.fine("Connected to server");

            buffIn = new BufferedInputStream(newSocket.getInputStream());
            buffOut = new BufferedOutputStream(newSocket.getOutputStream());
            log.finest("Set up io streams");

            /* Send the client version string. */
            buffOut.write(CLIENT_VERSION.getBytes(Charset.forName("US-ASCII")));
            buffOut.flush();

            log.finest("Written version to stream");

            /* Read the server version string. */
            byte[] serverVersionBytes = new byte[100];
            int readBytes = buffIn.read(serverVersionBytes);
            if (readBytes < 0) {
                throw new IOException("Couldn't read from stream");
            }
            String serverVersionStr = new String(serverVersionBytes, Charset.forName("US-ASCII")).substring(0, readBytes);

            log.finest("Read version from stream");

            log.config("Server version: " + serverVersionStr);
            log.config("Client version: " + CLIENT_VERSION);

            /* If the client and server version strings do not match, close the connection. */
            if (!serverVersionStr.startsWith(Util.VERSION_STR) || !serverVersionStr.endsWith("\r\n")) {
                log.warning("Client version differs from server version.");
                newSocket.close();
                return null;
            }

            return new PokerClientSocket(newSocket, buffIn, buffOut);

        } catch (IOException ioE) {
            log.logp(Level.SEVERE, getClass().getName(), "connectToServer", "Could not connect to server.", ioE);

            return null;
        }

    }

    /**
     * Try to connect to the table with the given table id.
     *
     * @param tableId The table id of the table to connect to.
     */
    private void connectToTable(int tableId) {

        /* If the given table id is already in the table list, we don't need to connect again. */
        if (tables.containsKey(tableId) && tables.get(tableId).isRunning() ) {
            log.fine("Can't connect to table, already exists.");
            //TODO try to get focus for the requested table.
            return;
        }

        /* Send a connect request to server for the given table. */
        clientSocket.writePacket(PokerPacket.createPacket(PacketCode.TABLE_CONNECT, tableId));

        /* Add an empty entry to the table list, ready for the response from the server. */
        tables.put(tableId, new ClientPokerTable(tableId ));
    }

    /**
     * Starts the Application, setting up all of the initial stage.
     * @param primaryStage The main Stage for the application.
     */
    @Override
    public void start(@NotNull Stage primaryStage) {

        log.finest("Poker Client started");

        /* Set the behaviour of the application when the primary stage is closed */
        primaryStage.setOnCloseRequest(event -> {

            if (tables.size() > 0) {
                /* If there are tables open, alert the user */
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "You have tables open. Still exit?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    /* If the user wants to close anyway, close all of the other open windows */
                    tables.forEach((tableId, table) -> table.quit());
                } else {
                    /* If the alert was cancelled, don't close the application. */
                    event.consume();
                }
            }

            /* When there are no other windows, close the socket and exit */
            if (clientSocket != null) {
                clientSocket.writePacket(PokerPacket.createPacket(PacketCode.DISCONNECT, DisconnectReason.USER_EXIT));
                //TODO we may want to do more when exiting.
                clientSocket.close();
            }

            Platform.exit();
        });

        /* Create the menu bar */
        MenuBar menuBar = new MenuBar();

        /* Create the File menu */
        Menu menu_file = new Menu("File");

        /* Create the options within file menu */
        MenuItem menu_file_connectToServer = new MenuItem("Connect to server");
        MenuItem menu_file_refreshTables = new MenuItem("Refresh Tables");
        MenuItem menu_file_connectToTable = new Menu("Connect to Table");//TODO we'll remove this once the table list is working.
        MenuItem menu_file_exit = new MenuItem("Exit");

        /* Set the behaviour of the options */
        /* "Connect to server" brings up a server address dialog to connect to and tries to connect. */
        menu_file_connectToServer.setOnAction(e -> {
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
                        int port = Util.DEFAULT_PORT;
                        /* If the user response contains a colon, the part after should be the port,
                         * and the part before the address.
                         */
                        if (response.contains(":")) {
                            port = Integer.parseInt(response.substring(response.indexOf(":") + 1));
                            response = response.substring(0, response.indexOf(":"));
                        }

                        /* Try to connect to the server. */
                        InetAddress address = InetAddress.getByName(response); //TODO I don't think we actually need to do this, the constructor should do it for us
                        PokerClientSocket newSocket = connectToServer(address.getHostAddress(), port);
                        /* If the socket is returned as null, the connection couldn't be completed. */
                        if (newSocket == null)
                            throw new IOException();

                        /* Set the socket. */
                        clientSocket = newSocket;
                        /* Enable the server related buttons in the application. */
                        menu_file_refreshTables.setDisable(false);
                        menu_file_connectToTable.setDisable(false);

                        break;

                    } catch (NumberFormatException | UnknownHostException ex) {
                        /* If the address is not valid, alert the user, then show the dialog again */
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error when connecting to server");
                        alert.setHeaderText("The address entered is not a valid address");
                        alert.showAndWait();
                        log.fine("Couldn't convert address to ip address");
                    } catch (IOException ex) {
                        /* If the connection to the server couldn't be completed, alert the user, then show
                         * the dialog again.
                         */
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error when connecting to server");
                        alert.setHeaderText("Could not connect to the server");
                        alert.showAndWait();
                        log.fine("Couldn't connect to server.");
                    }
                } else {
                    /* Otherwise, if the user entered an empty string or cancelled the dialog, we're done here. */
                    break;
                }
            } while (true);

        });

        /* "Refresh Tables" updates the table list from the server. */
        menu_file_refreshTables.setDisable(true);
        menu_file_refreshTables.setOnAction(e -> {
            if (clientSocket != null)
                clientSocket.writePacket(PokerPacket.createPacket(PacketCode.GLOBAL, GlobalCode.GET_TABLES));
        });

        /* TODO will be removed. */
        menu_file_connectToTable.setDisable(true);
        menu_file_connectToTable.setOnAction(e -> connectToTable(log.hashCode())); //TODO lul

        /* "Exit" just closes the app, in the same way as defined above */
        menu_file_exit.setOnAction(e -> primaryStage.close());

        /* Add all of the menu items together */
        menu_file.getItems().addAll(menu_file_connectToServer, menu_file_refreshTables, menu_file_exit);
        menuBar.getMenus().add(menu_file);

        /* Set the scene, and show it. */
        Scene scene = new Scene(new VBox(), 400, 350);
        ((VBox) scene.getRoot()).getChildren().addAll(menuBar);
        primaryStage.setScene(scene);
        primaryStage.show();


    }

    private class PokerClientSocket extends PokerSocket {

        PokerClientSocket(Socket s, BufferedInputStream in, BufferedOutputStream out) {
            super(s, in, out);
        }

        void tryAuthenticate(AuthMode mode) {
            if (mode == null) {
                setAuthenticated(false);
                log.fine("Unrecognized authentication type.");
                //TODO maybe disconnect from server
                return;
            }
            switch (mode) {
                case NONE:
                    /* If the AuthMode is NONE, we just send it back. */
                    writePacket(PokerPacket.createPacket(PacketCode.AUTH_DETAILS, AuthMode.NONE));
                    break;
                case PASSWORD:
                    /* If the AuthMode is PASSWORD, show a prompt to the user for input. */

                    Platform.runLater(() -> {
                        LoginDialog login = new LoginDialog();
                        Optional<Pair<String, String>> loginDetails = login.showAndWait();
                        //TODO we should check if the response is present, and do something different in that case
                        loginDetails.ifPresent(details -> writePacket(PokerPacket.createPacket(
                                PacketCode.AUTH_DETAILS, AuthMode.PASSWORD, details.getKey(), details.getValue())));

                    });
                    break;

                default:
                    /* If it is an unrecognized auth type, we cannot authenticate. */
                    setAuthenticated(false);
                    log.fine("Unrecognized authentication type.");
                    //TODO maybe disconnect from the server.
            }
        }

        @Override
        public void processCommand(PokerPacket pak) {

            log.finer("Trying to process command");

            switch (pak.getCode() ) {

                case AUTH_REQUIRED:
                    /* If we receive an AUTH_REQUIRED packet, check what type of auth is needed. */
                    AuthMode mode = AuthMode.getByVal(pak.getByte(1));

                    tryAuthenticate(mode);

                    break;

                case AUTH_SUCCESS:
                    /* If we receive AUTH_SUCCESS, set the connection to be authenticated. */
                    setAuthenticated(true);
                    log.fine("Successfully authenticated.");
                    break;

                case AUTH_FAIL:
                    /* If we receive AUTH_FAIL, see if we can continue with any other authentication. */
                    setAuthenticated(false);
                    AuthMode failedMode = AuthMode.getByVal(pak.getByte(1));
                    if (failedMode == null) {
                        log.fine("Could not get failed authentication mode.");
                        //TODO maybe disconnect from server
                        break;
                    }

                    Boolean canContinue;
                    AuthMode continueMode;
                    String failedUsername;
                    switch (failedMode) {
                        case PASSWORD:
                            /* If the failed authentication type was password, the username will be passed back. */
                            failedUsername = pak.getString(2);
                            log.fine("Could not authenticate with username " + failedUsername);
                            canContinue = pak.getBool(6 + failedUsername.length());
                            if (canContinue != null && canContinue) {
                                /* If the auth can continue, get the mode of the continuing auth. */
                                continueMode = AuthMode.getByVal(pak.getByte(7 + failedUsername.length()));

                                /* Send an alert to the user, and try the new mode. */
                                Platform.runLater(() -> {
                                    new Alert(Alert.AlertType.WARNING,
                                            "Could not authenticate with the server\nusing username " + failedUsername)
                                            .showAndWait();
                                    //TODO this is not so nice, as tryAuthenticate also calls Platform.runLater.
                                    // I don't know if they work well together.
                                    tryAuthenticate(continueMode);
                                });
                            }
                            break;
                        case NONE:
                            /* Should never be reached. */
                            log.fine("Somehow NONE authentication failed.");
                            canContinue = pak.getBool(3);
                            //TODO maybe alert the user
                            if (canContinue != null && canContinue) {
                                continueMode = AuthMode.getByVal(pak.getByte(4));
                                tryAuthenticate(continueMode);
                            }
                            break;
                        default:
                            log.fine("Unknown failed authentication method.");
                            canContinue = pak.getBool(3);
                            //TODO maybe alert the user
                            if (canContinue != null && canContinue) {
                                continueMode = AuthMode.getByVal(pak.getByte(4));
                                tryAuthenticate(continueMode);
                            }
                            break;
                    }
                    break;


                case TABLE_CONNECT_SUCCESS:
                    /* If we receive a TABLE_CONNECT_SUCCESS, add the table to the connected tables. */
                    Integer tableId = pak.getInteger(1);
                    if (tableId == null || tableId < 0) {
                        /* If we can't get the table id, we can't add the table. */
                        log.fine("Could not determine table number ot connect to.");
                        break;
                    }
                    if (!tables.containsKey(tableId) || tables.get(tableId).isRunning()) {
                        /* If we weren't expecting a table connect, or the table is already running, we can't add the table. */
                        log.fine("Unexpected connect to table " + tableId);
                        break;
                    }

                    /* Otherwise, retrieve the table from the list and set it running. */
                    tables.get(tableId).startRunning();
                    log.fine("Connected to table " + tableId);
                    break;

                case TABLE_CONNECT_FAIL:
                    /* If we receive TABLE_CONNECT_FAIL, remove the table from the list of connected tables. */

                    /* Get the table number. */
                    tableId = pak.getInteger(1);
                    if (tableId == null || tableId < 0) {
                        /* If we can't get the table id, we can't add the table. */
                        log.fine("Could not determine table number ot connect to.");
                        break;
                    }
                    if (!tables.containsKey(tableId) || tables.get(tableId).isRunning()) {
                        /* If we weren't trying to connect to the table, ignore it. */
                        log.fine("Unexpected connect to table " + tableId);
                        break;
                    }

                    /* Remove the table from the list. */
                    tables.remove(tableId);
                    log.fine("Could not connect to table " + tableId + ". Reason: " +
                            TableConnectFailCode.getByValue(pak.getByte(5)) + "; " + pak.getString(6));
                    break;
                case NONE:
                default:
                    /* If we don't recognize the command, we can't do anything. */
                    log.config("Could not recognize message code: " + pak.getCode().getVal());

            }

        }
    }

}
