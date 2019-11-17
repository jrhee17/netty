package io.netty.handler.codec.quic;

import org.junit.Test;

import static org.junit.Assert.*;

public class QuicResponseDecoderTest {

    @Test
    public void testHeaderForm() {
        assertTrue(QuicResponseDecoder.headerForm((byte) 0b10000000));
        assertFalse(QuicResponseDecoder.headerForm((byte) 0b00000000));
    }

    @Test
    public void testFixedBit() {
        assertTrue(QuicResponseDecoder.fixedBit((byte) 0b01000000));
        assertFalse(QuicResponseDecoder.fixedBit((byte) 0b00000000));
    }

    @Test
    public void testLongPacketType() {
        assertEquals(0, QuicResponseDecoder.longPacketType((byte) 0b00000000));
        assertEquals(1, QuicResponseDecoder.longPacketType((byte) 0b00010000));
        assertEquals(2, QuicResponseDecoder.longPacketType((byte) 0b00100000));
        assertEquals(3, QuicResponseDecoder.longPacketType((byte) 0b00110000));
    }
}
