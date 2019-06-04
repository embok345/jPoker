package space.poulter.poker.codes;

public enum AuthMode implements MessageCode {

    NONE(1),
    PASSWORD(2);

    private final byte val;

    AuthMode(int val) {
        this.val = (byte) val;
    }

    public static AuthMode getByVal(int val) {
        for (AuthMode mode : AuthMode.values()) {
            if (mode.val == val) return mode;
        }
        return NONE;
    }

    public byte getVal() {
        return val;
    }
}
