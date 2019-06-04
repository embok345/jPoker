package space.poulter.poker.server;

import space.poulter.poker.PokerPacket;
import space.poulter.poker.PokerSocket;
import space.poulter.poker.PokerTable;
import space.poulter.poker.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

class ServerPokerTable extends PokerTable {

    private final Logger log = Logger.getLogger(getClass().getName());
    private final List<PokerSocket> socketList = new ArrayList<>();

    {
        log.setLevel(Util.LOG_LEVEL);
    }

    ServerPokerTable(int tableId, byte noSeats) {
        super(tableId, noSeats, true);
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

    @Override
    protected void handlePacket(PokerPacket packet) {

    }
}
