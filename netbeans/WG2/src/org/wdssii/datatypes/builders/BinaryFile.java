package org.wdssii.datatypes.builders;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Utilities/class for direct reading of bytes from a data binary file There's
 * probably some tiny java class or free library out there that does this
 * fantastically. I'm just not aware of it at the moment.
 *
 * @author Robert Toomey
 */
public class BinaryFile {

    private final static Logger LOG = LoggerFactory.getLogger(BinaryFile.class);

    public static String logBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static int readShort(InputStream i) throws IOException {
        byte[] b = new byte[2];
        int value;
        int bytesRead = i.read(b, 0, 2);
        if (bytesRead == 2) {
            // What endian?
            value = ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
            //LOG.debug(logBytes(b) + " Read " + Integer.toString(value));
        } else {
            LOG.debug("Unexpected end of data file");
            value = 0;
            throw new EOFException();

        }
        return value;
    }

    public static int readUnsignedShort(InputStream i) throws IOException {
        byte[] b = new byte[2];
        int value;
        int bytesRead = i.read(b, 0, 2);
        if (bytesRead == 2) {
            // What endian?

            // value = (b[0] << 8) + b[1];
            // & with 0xff turns signed into 'unsigned' int
            value = ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
            //LOG.debug(logBytes(b) + "Read " + Integer.toString(value));
        } else {
            LOG.debug("Unexpected end of data file");
            value = 0;

            throw new EOFException();
        }
        return value;
    }

    public static int[] readUShortArray(InputStream i, int count) throws IOException {
        int[] is = new int[count];
        for (int x = 0; x < count; x++) {
            is[x] = readUnsignedShort(i);
        }
        return is;
    }

    public static int[] readShortArray(InputStream i, int count) throws IOException {
        int[] is = new int[count];
        for (int x = 0; x < count; x++) {
            is[x] = readShort(i);
        }
        return is;
    }

    /**
     * Returns an int of a char. This will be signed properly
     */
    public static int readChar(InputStream i) throws IOException {
        byte[] b = new byte[1];
        int value; // 32 bits to store 16 its unsigned...expanded.
        int bytesRead = i.read(b, 0, 1);
        if (bytesRead == 1) {
            // What endian?
            value = (b[0] & 0xFF);
            //LOG.debug(logBytes(b) + "Read " + Integer.toString(value));
        } else {
            LOG.debug("Unexpected end of data file");
            value = 0;

            throw new EOFException();
        }
        return value;
    }

    public static byte readRawChar(InputStream i) throws IOException {
        byte[] b = new byte[1];
        int bytesRead = i.read(b, 0, 1);
        if (bytesRead == 1) {
            // What endian?
            int value = (b[0] & 0xFF);
            //LOG.debug(logBytes(b) + "Read " + Integer.toString(value));
        } else {
            LOG.debug("Unexpected end of data file");
            b[0] = 0;
            throw new EOFException();
        }
        return b[0];
    }

    /**
     * Read a raw C++ unsigned byte into an array of byte. However this will be
     * incorrectly signed. We don't upcast to int because that increases space
     * requirements for large arrays. So we store as byte. To use the value need
     * to (byte & 0xFF) to int
     */
    public static byte[] readRawCharArray(InputStream i, int count) throws IOException {
        int non = 0;
        byte[] b = new byte[count];
        int bytesRead = i.read(b, 0, count);
        if (bytesRead == count) {
            for (int x = 0; x < count; x++) {
                if (b[x] == 0) {
                } else {
                    non++;
                }
            }
        } else {
            LOG.debug("Unexpected end of data...expected " + count + " and got " + bytesRead);
            throw new EOFException();
        }
        //LOG.debug("Read " + non + "NON ZERO VALUES" + count);
        return b;
    }

    public static long readUnsignedLong(InputStream i) throws IOException {
        byte[] b = new byte[4];
        long value;  // 64 bits to store 32 bits unsigned...expanded
        int bytesRead = i.read(b, 0, 4);
        if (bytesRead == 4) {
            value = ((b[3] & 0xFF) << 32)
                    | ((b[2] & 0xFF) << 16)
                    | ((b[1] & 0xFF) << 8)
                    | (b[0] & 0xFF);
           // LOG.debug(logBytes(b) + "Read " + Long.toString(value));
        } else {
            LOG.debug("Unexpected end of data file");

            value = 0;
            throw new EOFException();
        }
        return value;
    }
}
