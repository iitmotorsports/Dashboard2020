package sae.iit.saedashboard;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ByteSplit {

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);

    /**
     * Returns the hex string representation of the given byte array
     *
     * @param bytes byte array
     * @return The hex string
     */
    public static String hexStr(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /**
     * Reads the first two bytes of a message's array, composing them into an
     * unsigned short value
     *
     * @param data raw byte data
     * @return The unsigned short value as an int
     */
    public static int getUnsignedShort(byte[] data) {
        return (ByteBuffer.wrap(data).getShort() & 0xffff);
    }

    /**
     * Reads two bytes at the message's index, composing them into an unsigned short
     * value.
     *
     * @param data     raw byte data
     * @param position position in message array
     * @return The unsigned short value as an int
     */
    public static int getUnsignedShort(byte[] data, int position) {
        return (ByteBuffer.wrap(data).getShort(position) & 0xffff);
    }

    /**
     * Reads the first four bytes of a message's array, composing them into an
     * unsigned int value
     *
     * @param data raw byte data
     * @return The unsigned int value as a long
     */
    public static long getUnsignedInt(byte[] data) {
        return ((long) ByteBuffer.wrap(data).getInt() & 0xffffffffL);
    }

    /**
     * Reads four bytes at the message's index, composing them into an unsigned int
     * value.
     *
     * @param data     raw byte data
     * @param position position in message array
     * @return The unsigned short value at the buffer's current position as a long
     */
    public static long getUnsignedInt(byte[] data, int position) {
        return ((long) ByteBuffer.wrap(data).getInt(position) & 0xffffffffL);
    }

    /**
     * Gets the id numbers for a raw teensy byte array
     *
     * @param data raw byte data
     * @return the array of id numbers and number
     */
    public static long[] getTeensyMsg(byte[] data) { // TODO: check that the `get`s are done correctly
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int callerID = buf.getShort() & 0xffff;
        int stringID = buf.getShort() & 0xffff;
        long number = buf.getInt() & 0xffffffffL;
        long msgID = buf.getInt(0);

        return new long[]{callerID, stringID, number, msgID};
    }
}
