package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class QuicRequestTest {

    @Test
    public void test1byteEncoding() {
        final long length = 37;
        final byte[] retval = QuicRequest.variableLengthIntegerEncoding(length);
        assertEquals("25", ByteBufUtil.hexDump(retval));
    }

    @Test
    public void test1byteDecoding() {
        final ByteBuf byteBuf = Unpooled.copiedBuffer(ByteBufUtil.decodeHexDump("25"));
        final int result = QuicRequest.variableLengthIntegerDecoding(byteBuf);
        assertEquals(37, result);
    }

    @Test
    public void test2byteEncoding() {
        final long length = 15293;
        final byte[] retval = QuicRequest.variableLengthIntegerEncoding(length);
        assertEquals("7bbd", ByteBufUtil.hexDump(retval));
    }

    @Test
    public void test4byteEncoding() {
        final long length = 494878333;
        final byte[] retval = QuicRequest.variableLengthIntegerEncoding(length);
        assertEquals("9d7f3e7d", ByteBufUtil.hexDump(retval));
    }

    @Test
    public void test8byteEncoding() {
        final long length = 151288809941952652L;
        final byte[] retval = QuicRequest.variableLengthIntegerEncoding(length);
        assertEquals("c2197c5eff14e88c", ByteBufUtil.hexDump(retval));
    }
}
