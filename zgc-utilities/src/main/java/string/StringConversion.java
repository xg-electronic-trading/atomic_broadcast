package string;

import org.agrona.DirectBuffer;

public class StringConversion {

    public static long bufferToLong(DirectBuffer buffer, int offset, int fieldLength) {
        long x = 0;
        for (int i = Math.min(8, fieldLength) - 1; i >= 0; i--) {
            x <<= 8;
            x |= (buffer.getByte(offset + i) & 0xFF);
        }

        return x;
    }

    public static long stringToLong(CharSequence seq) {
        if (null == seq) {
            return 0;
        }

        long x = 0;
        for (int i = Math.min(8, seq.length()) - 1; i >= 0; i--) {
            x <<= 8;
            x |= (seq.charAt(i) & 0xFF);
        }

        return x;
    }

    public static String longToString(long l) {
        char[] chars = new char[8];
        int i = 0;
        while (l != 0 && i < 8) {
            chars[i] = (char) (l & 0xFF);
            i++;
            l >>= 8;
        }

        return new String(chars, 0, i);
    }

    public static int bufferToInt(DirectBuffer buffer, int offset, int fieldLength) {
        int x = 0;
        for (int i = Math.min(4, fieldLength) - 1; i >= 0; i--) {
            x <<= 8;
            x |= (buffer.getByte(offset + i) & 0xFF);
        }

        return x;
    }

    public static int stringToInt(CharSequence seq) {
        if (null == seq) {
            return 0;
        }

        int x = 0;
        for (int i = Math.min(4, seq.length()) - 1; i >= 0; i--) {
            x <<= 8;
            x |= (seq.charAt(i) & 0xFF);
        }

        return x;
    }

    public static String intToString(long l) {
        char[] chars = new char[4];
        int i = 0;
        while (l != 0 && i < 8) {
            chars[i] = (char) (l & 0xFF);
            i++;
            l >>= 8;
        }

        return new String(chars, 0, i);
    }
}
