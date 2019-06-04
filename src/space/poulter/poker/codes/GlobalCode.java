package space.poulter.poker.codes;

public enum GlobalCode implements MessageCode {

    NONE(0),

    GET_TABLES(1),
    TABLE_LIST(2);

    private final byte val;

    GlobalCode(int val) {
        this.val = (byte) val;
    }

    public static GlobalCode getByValue(int val) {
        for (GlobalCode code : GlobalCode.values()) {
            if (code.val == val)
                return code;
        }
        return NONE;
    }

    public byte getVal() {
        return val;
    }

}
