package space.poulter.poker;

import com.google.errorprone.annotations.Immutable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.poulter.poker.codes.MessageCode;
import space.poulter.poker.codes.PacketCode;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Immutable
public class PokerPacket {

    private final byte[] bytes;

    /**
     * Create a new Packet with the given bytes. Once the bytes are set, they cannot be changed; this class is Immutable.
     *
     * @param bytes The bytes to be in the packet.
     */
    @Contract(pure = true)
    PokerPacket(@NotNull byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Creates a packet with the given message code, and the given objects in the payload. It does no
     * checks to verify that the given message code expects the objects it is passed, and dutifully copies them
     * all in. It can only accept instances of ints and of Strings.
     * TODO should also accept most of the enum values.
     *
     * @param code    The message code that the packet should have.
     * @param objects The collection of objects that are to be encoded in the packet.
     * @return The packet formed from all of the objects, or a packet consisting of an empty array if any of
     * the objects are not valid.
     */
    @NotNull
    @Contract("_, _ -> new")
    public static PokerPacket createPacket(@NotNull PacketCode code, @NotNull Object... objects) {

        if (code == PacketCode.NONE) {
            return new PokerPacket(new byte[0]);
        }

        /* Create the list to be returned. */
        List<Byte> byteList = new ArrayList<>();

        /* Set the first byte as the given code value. */
        byteList.add(code.getVal());

        final Logger log = Logger.getLogger(PokerPacket.class.getName());
        log.setLevel(Util.LOG_LEVEL);

        /* Iterate through the Objects and append them to the list. */
        for (Object o : objects) {
            if (o instanceof Integer) {
                /* To add an int, convert it to a byte array, then add all of those bytes. */
                byteList.addAll(Arrays.asList(Util.intToBytes((Integer) o)));
            } else if (o instanceof String) {
                /* To add a string, first add the length, and then add the characters of the string as ASCII. */
                String str = (String) o;
                byteList.addAll(Arrays.asList(Util.intToBytes(str.length())));
                byte[] strBytes = str.getBytes(Charset.forName("US-ASCII"));
                //TODO surely there is a better way than adding one by one.
                for (byte b : strBytes) byteList.add(b);
            } else if (o instanceof Byte) {
                /* Adding a byte is easy. */
                byteList.add((Byte) o);
            } else if (o instanceof Boolean) {
                /* Adding a boolean is the same as adding a byte. */
                if ((Boolean) o) byteList.add((byte) 1);
                else byteList.add((byte) 0);
            } else if (o instanceof MessageCode) {
                /* To add the message code, just get the value from it. */
                byteList.add(((MessageCode) o).getVal());
            } else {
                /* If the Object is not of one of the correct types, we can't create the packet. */
                log.fine("Invalid Object in packet");
                return new PokerPacket(new byte[0]);
            }
        }
        /* Once we've added all of the objects to the list, convert it into an array.
         * TODO again, it seems there must be a better way of doing this. */
        byte[] byteArray = new byte[byteList.size()];
        int i = 0;
        for (Byte b : byteList) byteArray[i++] = b;

        /* Return the new packet. */
        return new PokerPacket(byteArray);
    }

    /**
     * Get the packet code from the packet. This is simply the first byte in the array. If the array is empty,
     * or if the code is not recognized, NONE is returned instead.
     *
     * @return The PacketCode for this packet, or NONE if it cannot be obtained.
     */
    public PacketCode getCode() {
        if (bytes.length == 0) return PacketCode.NONE;
        return PacketCode.getByValue(bytes[0]);
    }

    /**
     * Get an int from the given offset within the packet. That is convert the four bytes in the array starting from the
     * given offset into an int. This is read only as a unsigned int, so the first byte must be less than 128. If not
     * enough bytes are available, or the first byte is greater than 128, null is returned.
     *
     * @param offset The offset in the array to find the int.
     * @return The integer formed from the four bytes in the array from the given offset, or null if we can't get that.
     */
    @Nullable
    public Integer getInteger(int offset) {
        if (offset + 4 > bytes.length)
            return null;
        return Util.bytesToInt(Arrays.copyOfRange(bytes, offset, offset + 4));
    }

    /**
     * Get a String from the given offset within the packet. It first reads the length of the string, encoded as
     * a 31 bit integer, then converts that many bytes into a String, encoded as ASCII text. If we ever try to
     * read from outside the array, return the empty string.
     *
     * @param offset The offset in the array to find the String.
     * @return The String encoded within the packet, or the empty String if we cannot read the String.
     */
    @NotNull
    public String getString(int offset) {
        /* Get the length of the string, and make sure we can read all of the string. */
        int strLen = getInteger(offset);
        if (strLen < 0) return "";
        if (strLen + offset + 4 > bytes.length) return "";

        /* Convert the bytes into an ASCII String. */
        return new String(bytes, offset + 4, strLen, Charset.forName("US-ASCII"));

    }

    /**
     * Get the byte from the specified position in the array.
     *
     * @param index The index of the byte in the array to get.
     * @return The byte at the specified position, or -1 if the index is out of bounds. Note we only
     * ever expect positive bytes in the array.
     */
    @Nullable
    public Byte getByte(int index) {
        if (index >= bytes.length)
            return null;

        return bytes[index];
    }

    /**
     * Get the boolean value from the specified position in the packet. A boolean is encoded as 0 for false
     * and 1 for true, although any non-zero value is also interpreted as true. Returns null if the given index
     * is larger than the size of the array.
     *
     * @param index The index in the array in which to find the boolean.
     * @return The boolean in the given position, or null if the position is out of bounds.
     */
    @Nullable
    public Boolean getBool(int index) {
        if (index >= bytes.length)
            return null;
        return bytes[index] != 0;
    }

    /**
     * Get a copy of the underlying byte array of this object.
     *
     * @return A copy of the byte array in this object.
     */
    byte[] getBytes() {
        byte[] retBytes = new byte[bytes.length];
        System.arraycopy(bytes, 0, retBytes, 0, bytes.length);
        return retBytes;
    }

}
