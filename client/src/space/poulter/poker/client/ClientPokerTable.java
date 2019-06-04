package space.poulter.poker.client;

import space.poulter.poker.PokerPacket;
import space.poulter.poker.PokerTable;
import space.poulter.poker.Util;

import java.util.logging.Logger;

class ClientPokerTable extends PokerTable {

    private final Logger log = Logger.getLogger(getClass().getName());

    {
        log.setLevel(Util.LOG_LEVEL);
    }

    ClientPokerTable(int tableId) {
        super(tableId, (byte) 0, false);
    }

    ClientPokerTable(int tableId, byte noSeats) {
        super(tableId, noSeats, false);
    }

    void quit() {

    }

    @Override
    protected void handlePacket(PokerPacket packet) {

    }
}
