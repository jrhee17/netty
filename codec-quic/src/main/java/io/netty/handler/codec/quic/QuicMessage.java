package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;

public class QuicMessage {
    ByteBuf byteBuf;

    QuicMessage() {}

    public QuicMessage(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }
}
