package space.poulter.poker.codes;

public enum TableConnectFailCode implements MessageCode {

    GENERIC(1),
    DOES_NOT_EXIST(2),
    ALREADY_CONNECTED(3);

    private final byte val;

    TableConnectFailCode(int val) {
        this.val = (byte) val;
    }

    public static TableConnectFailCode getByValue(int val) {
        for (TableConnectFailCode code : TableConnectFailCode.values()) {
            if (code.val == val)
                return code;
        }
        return GENERIC;
    }

    public byte getVal() {
        return this.val;
    }

}
