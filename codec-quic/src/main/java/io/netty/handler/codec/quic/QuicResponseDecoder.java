package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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
        DCID_LEN,
        DCID,
        SCID_LEN,
        SCID,
    }

    private State state = State.INITIAL;
    private int dcidLen;
    private int scidLen;
    private int version;

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
            version = versionByteBuf.readInt();
            logger.info("version: {}", version);
            state = State.DCID_LEN;
        case DCID_LEN:
            if (!byteBuf.isReadable()) {
                return;
            }
            dcidLen = byteBuf.readByte();
            logger.info("dcidLen: {}", dcidLen);
            state = State.DCID;
        case DCID:
            if (!byteBuf.isReadable()) {
                return;
            }
            final ByteBuf dcid = byteBuf.readBytes(dcidLen);
            logger.info("dcid: {}", dcid);
            state = State.SCID_LEN;
        case SCID_LEN:
            if (!byteBuf.isReadable()) {
                return;
            }
            scidLen = byteBuf.readByte();
            logger.info("scidLen: {}", scidLen);
            state = State.SCID;
        case SCID:
            if (!byteBuf.isReadable()) {
                return;
            }
            final ByteBuf scid = byteBuf.readBytes(scidLen);
            logger.info("scid: {}", scid);
        }

        // it will be identified as a Version Negotiation packet based on the Version field having a value of 0
        if (version == 0) {
            while (byteBuf.readableBytes() > 0) {
                ByteBuf versionBytBuf = byteBuf.readBytes(4);
                logger.info("supported version hexdump: {}", ByteBufUtil.hexDump(versionBytBuf));
            }
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
