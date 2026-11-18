package jp.co.cyberagent.stf.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

public class FrameReaderTest {
    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }

        return result;
    }

    private static byte[] read(byte[] input) throws IOException {
        return FrameReader.readFrame(new ByteArrayInputStream(input));
    }

    @Test
    public void readsOneFrame() throws IOException {
        assertArrayEquals(bytes(7, 8, 9), read(bytes(3, 7, 8, 9, 1)));
    }

    @Test
    public void leavesTheNextFrameInTheStream() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes(1, 5, 2, 6, 7));

        assertArrayEquals(bytes(5), FrameReader.readFrame(in));
        assertArrayEquals(bytes(6, 7), FrameReader.readFrame(in));
        assertNull(FrameReader.readFrame(in));
    }

    @Test
    public void readsAnEmptyFrame() throws IOException {
        assertArrayEquals(new byte[0], read(bytes(0)));
    }

    @Test
    public void returnsNullAtTheEndOfTheStream() throws IOException {
        assertNull(read(new byte[0]));
    }

    @Test(expected = EOFException.class)
    public void failsOnATruncatedFrame() throws IOException {
        read(bytes(5, 1, 2));
    }

    @Test(expected = IOException.class)
    public void rejectsANegativeSize() throws IOException {
        read(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0x0F));
    }

    @Test(expected = IOException.class)
    public void rejectsAHugeSizeWithoutAllocatingIt() throws IOException {
        read(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0x07));
    }

    @Test(expected = IOException.class)
    public void rejectsASizeAboveTheCap() throws IOException {
        // 1 MiB + 1
        read(bytes(0x81, 0x80, 0x40));
    }

    @Test
    public void acceptsASizeAtTheCap() throws IOException {
        byte[] input = new byte[3 + FrameReader.MAX_FRAME_SIZE];
        input[0] = (byte) 0x80;
        input[1] = (byte) 0x80;
        input[2] = (byte) 0x40;

        assertArrayEquals(new byte[FrameReader.MAX_FRAME_SIZE], read(input));
    }
}
