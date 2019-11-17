package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class QuicResponseDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicResponseDecoder.class);

    private enum State {
        INITIAL,
        HEADER,
        DCID_LEN
    }

    private State state = State.INITIAL;

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        final ByteBuf byteBuf = msg.content();



        switch (state) {
        case INITIAL:
            if (!byteBuf.isReadable()) {
                return;
            }
            final byte headerByte = byteBuf.readByte();
            final boolean headerForm = headerForm(headerByte);

            // only process long header packets for now...
            if (!headerForm) {
                logger.info("headerForm: {}", headerForm);
                return;
            }

            final boolean fixedBit = fixedBit(headerByte);
            final short longPacketType = longPacketType(headerByte);
            final int typeSpecificBits = typeSpecificBits(headerByte);
            logger.info("headerForm: {}, fixedBit: {}, longPacketType: {}, typeSpecificBits: {}",
                        headerForm, fixedBit, longPacketType, typeSpecificBits);
            state = State.HEADER;
        case HEADER:
            if (!byteBuf.isReadable(4)) {
                return;
            }
            final ByteBuf versionByteBuf = byteBuf.readBytes(4);
            final int version = versionByteBuf.readInt();
            logger.info("version: {}", version);
            state = State.DCID_LEN;
        case DCID_LEN:
            if (!byteBuf.isReadable()) {
                return;
            }
            final byte dcidLenByte = byteBuf.readByte();
            logger.info("dcidLenByte: {}", dcidLenByte);

        }

        out.add(new QuicMessage(ReferenceCountUtil.retain(byteBuf)));
    }

    static int typeSpecificBits(byte b) {
        return b & 0x0f;
    }

    static short longPacketType(byte b) {
        return (short) ((b & 0x30) >> 4);
    }

    static boolean fixedBit(byte b) {
        return (b & 0x40) >> 6 == 1;
    }

    static boolean headerForm(byte b) {
        return (b & 0x80) >> 7 == 1;
    }
}
