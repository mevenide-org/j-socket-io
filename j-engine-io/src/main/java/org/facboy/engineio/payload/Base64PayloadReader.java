package org.facboy.engineio.payload;

import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.Packet;
import org.facboy.engineio.protocol.Packet.Type;
import org.facboy.engineio.protocol.StringPacket;

import com.google.common.io.BaseEncoding;

/**
 * @author Christopher Ng
 */
public class Base64PayloadReader implements PayloadReader {
    @Override
    public Iterable<Packet> readPayload(final InputStream inputStream) {
        return new Iterable<Packet>() {
            private Iterator<Packet> iterator;

            @Override
            public Iterator<Packet> iterator() {
                if (iterator == null) {
                    iterator = new PacketIterator(inputStream);
                }
                return iterator;
            }
        };
    }

    private static final Packet EOF = new BinaryPacket(Type.NOOP, new byte[0], 0, 0);
    private static final Type[] TYPE_VALUES = Type.values();

    private static class PacketIterator implements Iterator<Packet> {
        private final Reader reader;
        private final StringBuilder sb = new StringBuilder();
        private Packet next;

        public PacketIterator(InputStream inputStream) {
            try {
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean hasNext() {
            if (next == EOF) {
                return false;
            }

            try {
                if (next == null) {
                    // read the length
                    sb.delete(0, sb.length());
                    for (; ; ) {
                        int intC = reader.read();
                        if (intC == -1) {
                            next = EOF;
                            return false;
                        }
                        char c = (char) intC;
                        if (c == ':') {
                            break;
                        }
                        sb.append(c);
                    }

                    char[] buf = readData();

                    // determine type
                    int type = buf[0];
                    if (type == 'b') {
                        // means it's base-64 encoded binary
                        type = Character.getNumericValue(buf[1]);
                        byte[] data = BaseEncoding.base64().decode(CharBuffer.wrap(buf, 2, buf.length - 2));
                        next = new BinaryPacket(TYPE_VALUES[type], data, 0, data.length);
                    } else {
                        type = Character.getNumericValue(type);
                        next = new StringPacket(TYPE_VALUES[type], new String(buf, 1, buf.length - 1));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        private char[] readData() throws IOException {
            int length = Integer.parseInt(sb.toString());
            char[] buf = new char[length];
            int read = reader.read(buf);
            checkState(read == length, "Expected to read %s chars but only read %s", length, read);
            return buf;
        }

        @Override
        public Packet next() {
            if (next == EOF) {
                throw new NoSuchElementException();
            }
            if (next == null) {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
            }
            Packet local = next;
            next = null;
            return local;
        }
    }
}
