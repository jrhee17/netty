package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class QuicFrame {
    public static final byte CRYPTO_TYPE = 0x06;

    private final byte type;
    private final byte[] offset;
    private final byte[] length;
    private final byte[] data;

    public QuicFrame(byte type, byte[] offset, byte[] length, byte[] data) {
        this.type = type;
        this.offset = offset;
        this.length = length;
        this.data = data;
    }

    public ByteBuf toByteBuf() {
        return Unpooled.buffer().writeByte(type)
                .writeBytes(offset)
                .writeBytes(length)
                .writeBytes(data);
    }
}
