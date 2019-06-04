package space.poulter.poker;

import space.poulter.poker.codes.PacketCode;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public abstract class PokerTable {

    private final Logger log = Logger.getLogger(getClass().getName());
    private final Queue<PokerPacket> packetQueue = new LinkedList<>();
    private final int tableID;
    private final byte noSeats;
    private boolean isRunning = false;

    {
        log.setLevel(Util.LOG_LEVEL);
    }

    public PokerTable(int tableID, byte noSeats, boolean isRunning) {
        this.tableID = tableID;
        this.noSeats = noSeats;
        if (isRunning) {
            startRunning();
        }
    }

    public final void startRunning() {
        isRunning = true;
        new Thread(() -> {
            while (isRunning) {
                PokerPacket nextPacket;
                synchronized (packetQueue) {
                    while (packetQueue.isEmpty()) {
                        try {
                            packetQueue.wait();
                        } catch (InterruptedException ex) {

                        }
                    }
                    nextPacket = packetQueue.poll();
                }
                if (nextPacket == null) continue;

                /* nextPacket must be for this table, as `addPacketToQueue' checks this. */
                //TODO do stuff with the packet.
                log.finer("Handling packet for table " + tableID);
                handlePacket(nextPacket);
            }
        }).start();
    }

    public boolean isRunning() {
        return isRunning;
    }

    protected abstract void handlePacket(PokerPacket packet);

    public void addPacketToQueue(PokerPacket packet) {

        if (packet.getCode() != PacketCode.TABLE_DATA ||
                packet.getInteger(1) != tableID) {
            log.config("Table " + tableID + " received message not intended for it.");
            return;
        }

        synchronized (packetQueue) {
            packetQueue.add(packet);
            packetQueue.notify();
        }

    }
}
