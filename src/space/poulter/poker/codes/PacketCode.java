package space.poulter.poker.codes;

import org.jetbrains.annotations.Contract;

/**
 * The enum containing the message codes for communication packets. These codes are based on the
 * similar codes from the SSH protocol, somewhat arbitrarily. Each code is associated with a byte value, which
 * is what is sent in the packet.
 */
public enum PacketCode implements MessageCode {

    /* This should never be used. */
    NONE(0),

    /* For when either the client or the server wants to close the connection. */
    DISCONNECT(1),
    /* A catch all which will be sent if either party send a message with an unrecognized code. */
    UNIMPLEMENTED(2),

    /* Used for the authentication of the user when connecting to the server. */
    AUTH_REQUIRED(49),
    AUTH_DETAILS(50),
    AUTH_FAIL(51),
    AUTH_SUCCESS(52),

    /* Global catch all message code for when none other applies. */
    GLOBAL(80),

    /* Used for a client trying to connect to a new table. */
    TABLE_CONNECT(90),
    TABLE_CONNECT_FAIL(91),
    TABLE_CONNECT_SUCCESS(92),

    /* Used for all of the data related to a given table. */
    TABLE_DATA(93),

    /* Used when a client wants to close a table, or the server wants to kick the user from a table. */
    TABLE_CLOSE(97),
    TABLE_CLOSE_FAIL(98),
    TABLE_CLOSE_SUCCESS(99);

    /* The value associated with the enum, to be sent in a packet. */
    private final byte val;

    @Contract(pure = true)
    PacketCode(int val) {
        this.val = (byte) val;
    }

    /**
     * Finds the PacketCode associated with the given value. If it does not match any, it returns NONE.
     * TODO it seems to me that there should be some better way of doing this, but i can't see anything.
     * Also it would be nice for this to be in the interface, but it really needs to be static.
     *
     * @param val The value of PacketCode to match.
     * @return The PacketCode which is encoded as the given val, or NONE if there is no such PacketCode.
     */
    @Contract(pure = true)
    public static PacketCode getByValue(int val) {
        for (PacketCode code : PacketCode.values()) {
            if (code.val == val)
                return code;
        }
        return NONE;
    }

    @Contract(pure = true)
    public byte getVal() {
        return val;
    }
}
