package space.poulter.poker.codes;

import org.jetbrains.annotations.Contract;

public enum AuthMode implements MessageCode {

    /* NONE
     * no fields
     */
    NONE(1),

    /* PASSWORD
     * String username
     * String password
     */
    PASSWORD(2);

    private final byte val;

    @Contract(pure = true)
    AuthMode(int val) {
        this.val = (byte) val;
    }

    @Contract(value = "null -> null", pure = true)
    public static AuthMode getByVal(Byte val) {
        if (val == null) return null;
        for (AuthMode mode : AuthMode.values()) {
            if (mode.val == val) return mode;
        }
        return null;
    }

    public static AuthMode getByVal(int val) {
        return getByVal(new Byte((byte) val));
    }

    public byte getVal() {
        return val;
    }
}
