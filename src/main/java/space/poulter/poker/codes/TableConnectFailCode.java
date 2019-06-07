package space.poulter.poker.codes;

import org.jetbrains.annotations.Contract;

public enum TableConnectFailCode implements MessageCode {

    GENERIC(1),
    DOES_NOT_EXIST(2),
    ALREADY_CONNECTED(3);

    private final byte val;

    TableConnectFailCode(int val) {
        this.val = (byte) val;
    }

    public static TableConnectFailCode getByValue(int val) {
        return getByValue(Byte.valueOf((byte) val));
    }

    @Contract(value = "null -> null", pure = true)
    public static TableConnectFailCode getByValue(Byte val) {
        if (val == null) return null;
        for (TableConnectFailCode code : TableConnectFailCode.values()) {
            if (code.val == val) return code;
        }
        return null;
    }

    public byte getVal() {
        return this.val;
    }

}
