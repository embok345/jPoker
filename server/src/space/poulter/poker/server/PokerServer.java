package space.poulter.poker.server;

import org.jetbrains.annotations.NotNull;
import space.poulter.poker.PokerPacket;
import space.poulter.poker.PokerSocket;
import space.poulter.poker.Util;
import space.poulter.poker.codes.AuthMode;
import space.poulter.poker.codes.DisconnectReason;
import space.poulter.poker.codes.PacketCode;
import space.poulter.poker.codes.TableConnectFailCode;

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
    private final Map<Integer, PokerTable> tables = new HashMap<>();
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
        for (int i = 0; i < rand.nextInt(80) + 20; i++) {
            tables.put(i, new PokerTable(i, noSeats[rand.nextInt(3)]));
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

                    /* Do the authentication with the client. */
                    newPokerSocket.authenticate();

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

    /**
     * The socket for communication between the server and the client.
     */
    class PokerServerSocket extends PokerSocket {

        /**
         * A sublist of the keyset of tables which the client associated with this socket is connected to.
         */
        private final List<Integer> connectedTables = new ArrayList<>();

        PokerServerSocket(Socket s, BufferedInputStream in, BufferedOutputStream out) {
            super(s, in, out);
        }

        /**
         * Authenticate the user on the client side of the connection.
         */
        void authenticate() {
            /* Authentication differs based on the authentication mode of the server. */
            switch (authMode) {
                case NONE:
                    /* If the server uses no authentication, the user is automatically authenticated. */
                    setAuthenticated(true);
                    writePacket(PokerPacket.createPacket(PacketCode.AUTH_REQUIRED, AuthMode.NONE));
                    break;
                case PASSWORD:
                    /* If the server uses password authentication, send a request to the client for the username
                     * and password. */
                    writePacket(PokerPacket.createPacket(PacketCode.AUTH_REQUIRED, AuthMode.PASSWORD));
                    //TODO do the password authentication.
                    break;
            }
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

                case TABLE_DATA:
                    /* If the packet contains table data, try to send the packet on to the table. */
                    Integer tableId = pak.getInteger(1);
                    if (tableId < 0) {
                        log.config("Could not get table id from packet");
                        //TODO send response
                        break;
                    }
                    log.finer("Received packet for table " + tableId);

                    /* Make sure the id is correct, and that this socket is connected to the given table. */
                    PokerTable table = tables.get(tableId);

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

                    tableId = pak.getInteger(1);
                    if (tableId < 0) {
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
                    tableId = pak.getInteger(1);

                    /* Make sure that the table id is valid. */
                    if (tableId < 0) {
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
