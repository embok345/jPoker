package space.poulter.poker.codes;

import org.jetbrains.annotations.Contract;

public enum GlobalCode implements MessageCode {

    NONE(0),

    GET_TABLES(1),
    TABLE_LIST(2);

    private final byte val;

    GlobalCode(int val) {
        this.val = (byte) val;
    }

    @Contract(value = "null -> null", pure = true)
    public static GlobalCode getByValue(Byte val) {
        if (val == null) return null;
        for (GlobalCode code : GlobalCode.values()) {
            if (code.val == val) return code;
        }
        return null;
    }

    public static GlobalCode getByValue(int val) {
        return getByValue(Byte.valueOf((byte) val));
    }

    public byte getVal() {
        return val;
    }

}
