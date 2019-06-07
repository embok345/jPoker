package space.poulter.poker.server;

import org.jetbrains.annotations.NotNull;
import space.poulter.poker.PokerPacket;
import space.poulter.poker.PokerSocket;
import space.poulter.poker.Util;
import space.poulter.poker.codes.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class PokerServer {

    private static final String SERVER_VERSION = Util.VERSION_STR + "_server" + "\r\n";
    private static final int MAX_CLIENTS = 100;

    private final Logger log = Logger.getLogger(getClass().getName());
    private final AuthMode authMode = AuthMode.NONE;
    /**
     * List of client sockets which are currently connected to the server.
     */
    private final List<PokerServerSocket> sockets = new ArrayList<>();
    /**
     * Mapping of all of the tables currently available on the server, indexed by the table ids.
     */
    private final Map<Integer, ServerPokerTable> tables = new HashMap<>();
    private final ServerSocket serverSocket;

    {
        log.setLevel(Util.LOG_LEVEL);
    }

    /**
     * Create a server running on the default port given by Util.DEFAULT_PORT. Currently 1111.
     */
    PokerServer() {
        this(Util.DEFAULT_PORT);
    }

    /**
     * Create a server running on the given port.
     *
     * @param port The port for the server to listen on.
     */
    PokerServer(int port) {

        log.finest("Poker Server starting.");

        /* Create all of the tables. */
        Random rand = new Random();
        byte[] noSeats = {6, 8, 10};
        for (int i = 1; i < 10; i++) {
            tables.put(i, new ServerPokerTable(i, noSeats[rand.nextInt(3)]));
        }

        /* Create the server socket. */
        ServerSocket innerSock = null;
        try {
            innerSock = new ServerSocket(port);
            log.finest("Server socket started");
        } catch (IOException ioE) {
            /* If we can't open the server socket, we must exit. */
            log.logp(Level.SEVERE, this.getClass().getName(), "<init>", "Could not open server socket. Exiting", ioE);
            System.exit(1);
        } finally {
            serverSocket = innerSock;
        }

        /* Create a Thread to accept new connections to the server socket. */
        Thread waitForConnections = new Thread(() -> {
            while (true) {
                try {
                    /* Continually try to accept connection to the server. */
                    log.finest("Waiting for new connection");
                    Socket newSocket = serverSocket.accept();
                    log.fine("Accepted new connection");

                    BufferedOutputStream buffOut = new BufferedOutputStream(newSocket.getOutputStream());
                    BufferedInputStream buffIn = new BufferedInputStream(newSocket.getInputStream());
                    log.finest("Set up io streams");

                    /* Exchange the version strings. */
                    buffOut.write(SERVER_VERSION.getBytes(Charset.forName("US-ASCII")));
                    buffOut.flush();
                    log.finest("Written version to stream");
                    byte[] clientVersionBytes = new byte[100];
                    int readBytes = buffIn.read(clientVersionBytes);

                    /* If we couldn't read from the stream, close this new connection. */
                    if (readBytes < 0) {
                        log.warning("Could not read from the new stream.");
                        newSocket.close();
                        continue;
                    }
                    String clientVersionStr = new String(clientVersionBytes, Charset.forName("US-ASCII")).substring(0, readBytes);
                    log.config("Server version: " + SERVER_VERSION);
                    log.config("Client version: " + clientVersionStr);

                    /* If the versions don't match, close the new connection. */
                    if (!clientVersionStr.startsWith(Util.VERSION_STR) || !clientVersionStr.endsWith("\r\n")) {
                        log.warning("Client version differs from server version.");
                        newSocket.close();
                        continue;
                    }

                    /* Start up the new encapsulated socket. */
                    PokerServerSocket newPokerSocket = new PokerServerSocket(newSocket, buffIn, buffOut);

                    if (sockets.size() >= MAX_CLIENTS) {
                        /* If there are too many clients connected, disconnect. */
                        newPokerSocket.writePacket(PokerPacket.createPacket(PacketCode.DISCONNECT, DisconnectReason.TOO_MANY_CONNECTIONS,
                                "There are currently too many connections to the server, try again later."));
                        continue;
                    }

                    sockets.add(newPokerSocket);
                    log.config("Started new connection with " + newSocket.getInetAddress().toString());

                    /* Start the authentication with the client. */
                    newPokerSocket.startAuthentication();

                } catch (IOException ioE) {
                    if (serverSocket.isClosed()) {
                        /* If the server socket is closed, we need to exit. */
                        log.severe("Server socket was closed. Exiting");
                        System.exit(1);
                    } else {
                        log.warning("New connection to server failed.");
                    }
                }
            }
        });
        waitForConnections.start();

    }

    private boolean authenticateUser(String username, String password) {
        return username.equals("a") && password.equals("b");
    }

    /**
     * The socket for communication between the server and the client.
     */
    private class PokerServerSocket extends PokerSocket {

        /**
         * A sublist of the key set of tables which the client associated with this socket is connected to.
         */
        private final List<Integer> connectedTables = new ArrayList<>();

        PokerServerSocket(Socket s, BufferedInputStream in, BufferedOutputStream out) {
            super(s, in, out);
        }

        /**
         * Authenticate the user on the client side of the connection.
         */
        void startAuthentication() {
            /* Authentication differs based on the authentication mode of the server. */
            if (authMode == AuthMode.NONE) {
                /* If the server uses no authentication, the user is automatically authenticated. */
                completeAuthentication();
            } else {
                /* Otherwise send a request for the given auth type. */
                log.finer("Sending auth required");
                writePacket(PokerPacket.createPacket(PacketCode.AUTH_REQUIRED, authMode));
            }
        }

        /**
         * Finish the authentication with the user.
         */
        void completeAuthentication() {
            setAuthenticated(true);
            writePacket(PokerPacket.createPacket(PacketCode.AUTH_SUCCESS));
            log.fine("Completed authentication of user.");

            /* Send the list of tables available */
            sendTableList();

        }

        /**
         * Send the list of tables available to the client.
         */
        private void sendTableList() {
            /* Form an initial packet consisting of the codes, and the number of tables to send. */
            PokerPacket tablesPacket = PokerPacket.createPacket(PacketCode.GLOBAL, GlobalCode.TABLE_LIST,
                    tables.size());
            /* Then, add the tableId, number of seats, and number of occupied seats, for each table. */
            for (Map.Entry<Integer, ServerPokerTable> entry : tables.entrySet()) {
                tablesPacket = tablesPacket.addAll(entry.getKey(), entry.getValue().getNoSeats(), entry.getValue().getOccupiedSeats());
                //TODO there may be more information we want to send.
            }
            writePacket(tablesPacket);
        }

        /**
         * Process the packet received from the client.
         *
         * @param pak The packet to process.
         */
        @Override
        public void processCommand(@NotNull PokerPacket pak) {

            log.finer("Trying to process packet.");

            switch (pak.getCode()) {

                case AUTH_DETAILS:
                    /* Try to authenticate the user with the given details. */

                    AuthMode mode = AuthMode.getByVal(pak.getByte(1));

                    /* If we couldn't get the mode, reject the authentication. */
                    if (mode == null) {
                        setAuthenticated(false);
                        writePacket(PokerPacket.createPacket(PacketCode.AUTH_FAIL, AuthMode.NONE, true, authMode));
                        log.fine("Could not authenticate: unrecognized authentication type.");
                        break;
                    }

                    /* If the auth request is not of the same type as required by the server, reject the auth.
                     * Allow the correct auth type to continue. */
                    //TODO we should really lower bound the authentication type.
                    if (mode != authMode) {
                        setAuthenticated(false);
                        writePacket(PokerPacket.createPacket(PacketCode.AUTH_FAIL, mode, true, authMode));
                        log.fine("Could not authenticate using mode " + mode);
                        break;
                    }

                    /* Otherwise check the authentication. */
                    switch (mode) {
                        case PASSWORD:
                            /* If the authentication type is password based, verify this. */
                            /* Get the username and password. */
                            String username = pak.getString(2);
                            String password = pak.getString(6 + username.length());

                            if (PokerServer.this.authenticateUser(username, password)) {
                                /* If the given username and password are correct, set the connection authenticated. */
                                completeAuthentication();
                            } else {
                                /* Otherwise, send a fail packet to the user, and allow them to try again.
                                 * TODO we should limit the number of retries. */
                                setAuthenticated(false);
                                writePacket(PokerPacket.createPacket(PacketCode.AUTH_FAIL, mode, username,
                                        true, authMode));
                                log.fine("Could not authenticate user " + username);
                            }
                            break;

                        case NONE:
                            /* If the authentication is NONE, set authenticated. This should never be reached. */
                            completeAuthentication();
                            break;

                        default:
                            /* If the authentication type is anything else, we reject it. */
                            setAuthenticated(false);
                            writePacket(PokerPacket.createPacket(PacketCode.AUTH_FAIL, mode, true, authMode));
                            log.fine("Could not authenticate, unrecognized authentication type.");
                    }
                    break;

                case GLOBAL:
                    /* Respond to the given global request. */
                    if (!isAuthenticated()) {
                        /* If the user isn't authenticated, reject the packet. */
                        log.config("Cannot accept packet from unauthenticated user.");
                        startAuthentication();
                        break;
                    }

                    /* Get the Global Message Code. */
                    GlobalCode globalCode = GlobalCode.getByValue(pak.getByte(1));

                    if (globalCode == null) {
                        log.fine("Could not get Global Message code");
                        //TODO send error packet
                        break;
                    }

                    switch (globalCode) {
                        case GET_TABLES:
                            sendTableList();
                            break;

                        case NONE:
                        default:
                            log.fine("Unrecognized Global Message code: " + globalCode);
                    }

                    break;

                case TABLE_DATA:
                    /* If the packet contains table data, try to send the packet on to the table. */

                    if (!isAuthenticated()) {
                        /* If the user isn't authenticated, reject the packet. */
                        log.config("Cannot accept packet from unauthenticated user.");
                        startAuthentication();
                        break;
                    }

                    Integer tableId = pak.getInteger(1);
                    if (tableId == null || tableId < 0) {
                        log.config("Could not get table id from packet");
                        //TODO send response
                        break;
                    }
                    log.finer("Received packet for table " + tableId);

                    /* Make sure the id is correct, and that this socket is connected to the given table. */
                    ServerPokerTable table = tables.get(tableId);

                    if (table == null) {
                        log.config("Table with given id doesn't exist.");
                        //TODO send response
                        break;
                    }
                    if (!connectedTables.contains(tableId) || !table.hasConnectedSocket(this)) {
                        log.config("Socket is not connected to table.");
                        //TODO send response
                        break;
                    }

                    /* Hand off the packet to the table. */
                    table.addPacketToQueue(pak);

                    break;

                case TABLE_CLOSE:
                    /* Try to close the connection to the table. */

                    if (!isAuthenticated()) {
                        /* If the user isn't authenticated, reject the packet. */
                        log.config("Cannot accept packet from unauthenticated user.");
                        startAuthentication();
                        break;
                    }

                    tableId = pak.getInteger(1);
                    if (tableId == null || tableId < 0) {
                        log.config("Could not get table id from packet");
                        //TODO send response.
                        //TABLE_CLOSE_FAIL
                        break;
                    }
                    log.finer("Trying to close table " + tableId);

                    /* Make sure the id is correct, and that this socket is connected to the given table. */
                    table = tables.get(tableId);

                    if (table == null) {
                        log.config("Table with given id doesn't exist.");
                        //TODO send response
                        //TABLE_CLOSE_FAIL
                        break;
                    }
                    if (!connectedTables.contains(tableId) || !table.hasConnectedSocket(this)) {
                        log.config("Socket is not connected to table.");
                        //TODO send response
                        //TABLE_CLOSE_FAIL
                        break;
                    }

                    /* Remove the socket from the table. */
                    table.removeConnectedSocket(this);
                    connectedTables.remove(tableId);

                    //TODO send response
                    //TABLE_CLOSE_SUCCESS

                    log.finer("Closed table " + tableId);

                    break;

                case TABLE_CONNECT:
                    /* Try to connect this socket to the table. */

                    if (!isAuthenticated()) {
                        /* If the user isn't authenticated, reject the packet. */
                        log.config("Cannot accept packet from unauthenticated user.");
                        startAuthentication();
                        break;
                    }

                    tableId = pak.getInteger(1);

                    /* Make sure that the table id is valid. */
                    if (tableId == null || tableId < 0) {
                        log.config("Could not get table id from packet");
                        writePacket(PokerPacket.createPacket(PacketCode.TABLE_CONNECT_FAIL, tableId,
                                TableConnectFailCode.DOES_NOT_EXIST, "Invalid table number: " + tableId));
                        break;
                    }

                    /* Make sure that the table id is one which exists. */
                    if (!tables.containsKey(tableId)) {
                        log.config("Invalid table number.");
                        writePacket(PokerPacket.createPacket(PacketCode.TABLE_CONNECT_FAIL, tableId,
                                TableConnectFailCode.DOES_NOT_EXIST, "Table number does not exist: " + tableId));
                        break;
                    }
                    log.finer("Trying to connect to table " + tableId);

                    table = tables.get(tableId);
                    /* Make sure that this client isn't already attached to this table. */
                    if (connectedTables.contains(tableId) || table.hasConnectedSocket(this)) {
                        log.config("Already connected to table.");
                        writePacket(PokerPacket.createPacket(PacketCode.TABLE_CONNECT_FAIL, tableId,
                                TableConnectFailCode.ALREADY_CONNECTED, "Can't connect to tables already connected to."));
                        break;
                    }

                    /* Add this socket to the table. */
                    table.addConnectedSocket(this);
                    connectedTables.add(tableId);

                    log.finer("Connected to table " + tableId);
                    writePacket(PokerPacket.createPacket(PacketCode.TABLE_CONNECT_SUCCESS, tableId));

                    break;

                case DISCONNECT:
                    /* If the packet is a disconnect packet, disconnect this socket.
                     * TODO disconnect from all of the related things. */
                    log.fine("Closing connection");

                    close();
                    sockets.remove(this);
                    break;

                case NONE:
                default:
                    /* If we do not recognize the message, ignore it. */
                    log.config("Could not get message code from packet.");
                    //TODO send unimplemented code.
            }

        }
    }
}
