package space.poulter.poker.server;

import space.poulter.poker.PokerPacket;
import space.poulter.poker.PokerSocket;
import space.poulter.poker.Util;
import space.poulter.poker.codes.PacketCode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

class PokerTable {

    private final Logger log = Logger.getLogger(getClass().getName());
    private final List<PokerSocket> socketList = new ArrayList<>();
    private final Queue<PokerPacket> packetQueue = new LinkedList<>();
    private final int tableID;
    private final byte noSeats;

    {
        log.setLevel(Util.LOG_LEVEL);
    }

    PokerTable(int tableID, byte noSeats) {
        this.tableID = tableID;
        this.noSeats = noSeats;

        Thread packetHandler = new Thread(() -> {
            while (true) {
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
            }
        });
        packetHandler.start();
    }

    boolean hasConnectedSocket(PokerSocket socket) {
        return socketList.contains(socket);
    }

    void addConnectedSocket(PokerSocket socket) {
        if (!socketList.contains(socket)) socketList.add(socket);
    }

    void removeConnectedSocket(PokerSocket socket) {
        socketList.remove(socket);
    }

    void addPacketToQueue(PokerPacket packet) {

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
