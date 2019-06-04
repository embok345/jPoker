package space.poulter.poker.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
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

    private static final String CLIENT_VERSION = Util.VERSION_STR + "_client" + "\r\n";

    private final Logger log = Logger.getLogger(getClass().getName());
    private Map<Integer, ClientPokerTable> tables = new HashMap<>();
    private PokerClientSocket clientSocket;

    {
        log.setLevel(Util.LOG_LEVEL);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void init(Stage primaryStage) {

        log.finest("Poker Client started");

        /* Set the behaviour of the application when the primary stage is closed */
        primaryStage.setOnCloseRequest(event -> {

            if (tables.size() > 0) {
                /* If there are tables open, alert the user */
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "You have tables open. Still exit?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    /* If the user wants to close anyway, close all of the other open windows */
                    //windows.forEach((tableID, table) -> table.quit());

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
                        int port = Util.DEFAULT_PORT;
                        /* If the user response contains a colon, the part after should be the port,
                         * and the part before the address.
                         */
                        if (response.contains(":")) {
                            port = Integer.parseInt(response.substring(response.indexOf(":") + 1));
                            response = response.substring(0, response.indexOf(":"));
                        }

                        /* Try to connect to the server. */
                        //TODO I don't think we actually need to do this, the constructor should do it for us.
                        InetAddress address = InetAddress.getByName(response);
                        PokerClientSocket newSocket = connectToServer(address.getHostAddress(), port);
                        /* If the socket is returned as null, the connection couldn't be completed. */
                        if (newSocket == null)
                            throw new IOException();

                        clientSocket = newSocket;

                        /* Now that we have a connection to the server, get the table list. */
                        clientSocket.writePacket(PokerPacket.createPacket(PacketCode.GLOBAL, GlobalCode.GET_TABLES));

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
     * Try to connect to the server with the given ip address and port
     *
     * @param ip
     * @param port
     * @return
     */
    private PokerClientSocket connectToServer(String ip, int port) {

        Socket newSocket;
        BufferedInputStream buffIn;
        BufferedOutputStream buffOut;
        PokerClientSocket newPokerSocket = null;

        try {
            newSocket = new Socket(ip, port);
            log.fine("Connected to server");

            buffIn = new BufferedInputStream(newSocket.getInputStream());
            buffOut = new BufferedOutputStream(newSocket.getOutputStream());
            log.finest("Set up io streams");

            buffOut.write(CLIENT_VERSION.getBytes(Charset.forName("US-ASCII")));
            buffOut.flush();

            log.finest("Written version to stream");

            byte[] serverVersionBytes = new byte[100];
            int readBytes = buffIn.read(serverVersionBytes);
            if (readBytes < 0) {
                throw new IOException("Couldn't read from stream");
            }
            String serverVersionStr = new String(serverVersionBytes, Charset.forName("US-ASCII")).substring(0, readBytes);

            log.finest("Read version from stream");

            log.config("Server version: " + serverVersionStr);
            log.config("Client version: " + CLIENT_VERSION);

            if (!serverVersionStr.startsWith(Util.VERSION_STR) || !serverVersionStr.endsWith("\r\n")) {
                log.warning("Client version differs from server version.");
                newSocket.close();
                return null;
            }

            newPokerSocket = new PokerClientSocket(newSocket, buffIn, buffOut);

        } catch (IOException ioE) {
            log.logp(Level.SEVERE, getClass().getName(), "connectToServer", "Could not connect to server.", ioE);
        }

        return newPokerSocket;
    }

    private void connectToTable(int tableId) {

        if (tables.containsKey(tableId) && tables.get(tableId) != null) {
            log.fine("Can't connect to table, already exists.");
            return;
        }
        clientSocket.writePacket(PokerPacket.createPacket(PacketCode.TABLE_CONNECT, tableId));
        tables.put(tableId, null);
    }

    @Override
    public void start(Stage primaryStage) {
        init(primaryStage);
    }

    private class PokerClientSocket extends PokerSocket {

        PokerClientSocket(Socket s, BufferedInputStream in, BufferedOutputStream out) {
            super(s, in, out);
        }

        @Override
        public void processCommand(PokerPacket pak) {

            log.finer("Trying to process command");

            switch (pak.getCode()) {

                case AUTH_REQUIRED:
                    /* If we receive an AUTH_REQUIRED packet, check what type of auth is needed. */
                    AuthMode authMode = AuthMode.getByVal(pak.getByte(1));
                    if (authMode == AuthMode.NONE) {
                        /* If the AuthMode is NONE, we don't need to do anything. */
                        setAuthenticated(true);
                        break;
                    }
                    if (authMode == AuthMode.PASSWORD) {
                        /* If the AuthMode is PASSWORD, show a prompt to the user for input. */

                    }

                case TABLE_CONNECT_SUCCESS:
                    int tableId = pak.getInteger(1);
                    if (tableId < 0) {
                        log.fine("Could not determine table number ot connect to.");
                        break;
                    }
                    if (!tables.containsKey(tableId) || tables.get(tableId) != null) {
                        log.fine("Unexpected connect to table " + tableId);
                        break;
                    }
                    tables.put(tableId, new ClientPokerTable());
                    log.fine("Connected to table " + tableId);
                    break;

                case TABLE_CONNECT_FAIL:
                    tableId = pak.getInteger(1);
                    if (tableId < 0) {
                        log.fine("Could not determine table number ot connect to.");
                        break;
                    }
                    if (!tables.containsKey(tableId) || tables.get(tableId) != null) {
                        log.fine("Unexpected connect to table " + tableId);
                        break;
                    }
                    tables.remove(tableId, null);
                    log.fine("Could not connect to table " + tableId + ". Reason: " +
                            TableConnectFailCode.getByValue(pak.getByte(5)) + "; " + pak.getString(6));
                    break;
                case NONE:
                default:
                    log.config("Could not recognize message code: " + pak.getCode().getVal());

            }

        }
    }

}
