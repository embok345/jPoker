package space.poulter.poker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Em Poulter
 */
public abstract class PokerSocket implements Closeable {

    protected final Logger log = Logger.getLogger(getClass().getName());
    private final Socket s;
    private final BufferedOutputStream out;
    private final BufferedInputStream in;
    private volatile boolean connectionLive;
    /**
     * Thread to continuously read packets from the input stream. Adds new read packets to the
     * Queue of packets to be dealt with.
     */
    private final Thread reader = new Thread(() -> {
        /* Keep reading packets while the connection is open. */
        while (connectionLive) {
            /* Try to read a packet from the stream. */
            PokerPacket newPacket = readPacket();
            if (newPacket != null) {
                /* If we do read a packet, deal with it. */
                log.finer("Read new packet from stream");
                processCommand(newPacket);
            }
        }
    });
    private volatile boolean isAuthenticated = false;

    {
        log.setLevel(Util.LOG_LEVEL);
    }

    public PokerSocket(Socket s, BufferedInputStream in, BufferedOutputStream out) {
        this.s = s;
        this.in = in;
        this.out = out;
        connectionLive = true;
        reader.start();
    }

    public PokerSocket(Socket s) throws IOException {
        this.s = s;
        out = new BufferedOutputStream(s.getOutputStream());
        in = new BufferedInputStream(s.getInputStream());
        connectionLive = true;
        reader.start();
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    @Override
    public void close() {
        connectionLive = false;
        try {
            s.close();
        } catch (IOException e) {
            log.logp(Level.WARNING, getClass().getName(), "close", "Could not close socket", e);
        }
    }

    public void write(Object o) throws IOException {
    }

    private void writePacket(byte[] bytes) {
        if (!connectionLive) {
            log.warning("Could not send packet as the connection is closed.");
            return;
        }

        byte[] lenBytes = Util.intToBytesPrim(bytes.length);
        byte[] totalBytes = new byte[4 + bytes.length];
        System.arraycopy(lenBytes, 0, totalBytes, 0, 4);
        System.arraycopy(bytes, 0, totalBytes, 4, bytes.length);
        try {
            out.write(totalBytes);
            out.flush();
        } catch (IOException ioE) {
            log.logp(Level.WARNING, getClass().getName(), "writePacket", "Could not erite packet to stream", ioE);
        }
    }

    public final void writePacket(PokerPacket packet) {
        writePacket(packet.getBytes());
    }

    private PokerPacket readPacket() {
        int readBytes = 0;
        try {
            byte[] lenBytes = new byte[4];
            readBytes = in.read(lenBytes);
            if (readBytes != 4) {
                throw new IOException("Not enough bytes read from socket");
            }
            int len = Util.bytesToInt(lenBytes);
            if (len <= 0) throw new IOException("Could not get length of packet.");

            byte[] packetBytes = new byte[len];
            readBytes = in.read(packetBytes);
            if (readBytes != len) {
                throw new IOException("Not enough bytes read from socket");
            }
            return new PokerPacket(packetBytes);
        } catch (IOException ex) {
            if (readBytes == -1 && connectionLive) {
                log.logp(Level.WARNING, getClass().getName(), "readPacket", "Socket has been closed");
                connectionLive = false;
                return null;
            }
            if (ex instanceof SocketException && connectionLive) {
                log.logp(Level.WARNING, getClass().getName(), "readPacket", "Socket has been closed");
                connectionLive = false;
                return null;
            }
            if (connectionLive)
                log.logp(Level.WARNING, getClass().getName(), "readPacket", "Could not read packet", ex);
            return null;
        }
    }

    public abstract void processCommand(PokerPacket pak);
}
