package org.facboy.engineio;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.io.BaseEncoding;

/**
 * @author Christopher Ng
 */
public class Base64IdGenerator implements IdGenerator {
    private final AtomicInteger sequenceNumber = new AtomicInteger();
    private static final ThreadLocal<SecureRandom> random = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    };

    @Override
    public String generateId() {
        ByteBuffer id = ByteBuffer.allocate(15);
        id.putInt(11, sequenceNumber.getAndIncrement());
        random.get().nextBytes(id.array());
        return BaseEncoding.base64Url().encode(id.array());
    }
}
