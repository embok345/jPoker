package space.poulter.poker.codes;

public enum DisconnectReason implements MessageCode {

    NONE(0),

    USER_EXIT(1),

    TOO_MANY_CONNECTIONS(2);


    private final byte val;

    DisconnectReason(int val) {
        this.val = (byte) val;
    }

    public static DisconnectReason getByValue(int val) {
        for (DisconnectReason code : DisconnectReason.values()) {
            if (code.val == val)
                return code;
        }
        return NONE;
    }

    public byte getVal() {
        return val;
    }


}
