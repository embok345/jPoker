package space.poulter.poker

import space.poulter.poker.codes.PacketCode

class PokerPacketTest extends GroovyTestCase {

    PokerPacket packet1

    void testCreatePacket() {

        packet1 = PokerPacket.createPacket(PacketCode.GLOBAL, "string1", 1234, "string2", false, "string3")
        packet1.printPacket()


    }

    void testAddAll() {

        testCreatePacket()
        packet1.addAll(true, 9876, "string4").printPacket()


    }
}
