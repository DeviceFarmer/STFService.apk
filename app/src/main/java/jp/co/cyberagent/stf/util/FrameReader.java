package jp.co.cyberagent.stf.util;

import com.google.protobuf.CodedInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class FrameReader {
    /**
     * Requests are small. The cap keeps a bad length prefix from making the agent allocate it.
     */
    static final int MAX_FRAME_SIZE = 1024 * 1024;

    /**
     * Reads one length-delimited message without parsing it, so that a message that cannot
     * be parsed does not leave the stream in the middle of it.
     *
     * @return the message, or null at the end of the stream
     * @throws IOException if the length is invalid or the stream ends in the middle of a message
     */
    public static byte[] readFrame(InputStream in) throws IOException {
        int firstByte = in.read();

        if (firstByte == -1) {
            return null;
        }

        int size = CodedInputStream.readRawVarint32(firstByte, in);

        if (size < 0 || size > MAX_FRAME_SIZE) {
            throw new IOException("Invalid request size " + size);
        }

        byte[] frame = new byte[size];
        int offset = 0;

        while (offset < frame.length) {
            int count = in.read(frame, offset, frame.length - offset);

            if (count == -1) {
                throw new EOFException("Connection closed in the middle of a request");
            }

            offset += count;
        }

        return frame;
    }
}
