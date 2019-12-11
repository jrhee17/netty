package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public abstract class QuicObjectDecoder extends MessageToMessageDecoder<DatagramPacket> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicObjectDecoder.class);

    private enum State {
        INITIAL,
        HEADER,
        DCID_LEN,
        DCID,
        SCID_LEN,
        SCID,
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

    private State state = State.INITIAL;
    private int dcidLen;
    private int scidLen;
    private int version;

    protected boolean parseLongPacketHeader(ByteBuf byteBuf) {
        switch (state) {
        case INITIAL:
            if (!byteBuf.isReadable()) {
                return true;
            }
            final byte headerByte = byteBuf.readByte();
            final boolean headerForm = headerForm(headerByte);

            // only process long header packets for now...
            if (!headerForm) {
                logger.info("headerForm: {}", headerForm);
                return true;
            }

            final boolean fixedBit = fixedBit(headerByte);
            final short longPacketType = longPacketType(headerByte);
            final int typeSpecificBits = typeSpecificBits(headerByte);
            logger.info("headerForm: {}, fixedBit: {}, longPacketType: {}, typeSpecificBits: {}",
                        headerForm, fixedBit, longPacketType, typeSpecificBits);
            state = State.HEADER;
        case HEADER:
            if (!byteBuf.isReadable(4)) {
                return true;
            }
            final ByteBuf versionByteBuf = byteBuf.readBytes(4);
            version = versionByteBuf.copy().readInt();
            logger.info("version: {}", ByteBufUtil.prettyHexDump(versionByteBuf));
            state = State.DCID_LEN;
        case DCID_LEN:
            if (!byteBuf.isReadable()) {
                return true;
            }
            dcidLen = byteBuf.readByte();
            logger.info("dcidLen: {}", dcidLen);
            state = State.DCID;
        case DCID:
            if (!byteBuf.isReadable(dcidLen)) {
                return true;
            }
            final ByteBuf dcid = byteBuf.readBytes(dcidLen);
            logger.info("dcid: {}", ByteBufUtil.prettyHexDump(dcid));
            state = State.SCID_LEN;
        case SCID_LEN:
            if (!byteBuf.isReadable()) {
                return true;
            }
            scidLen = byteBuf.readByte();
            logger.info("scidLen: {}", scidLen);
            state = State.SCID;
        case SCID:
            if (!byteBuf.isReadable(scidLen)) {
                return true;
            }
            final ByteBuf scid = byteBuf.readBytes(scidLen);
            logger.info("scid: {}", ByteBufUtil.prettyHexDump(scid));
        }

        logger.info("remaining bytes: {}", ByteBufUtil.prettyHexDump(byteBuf));

        // it will be identified as a Version Negotiation packet based on the Version field having a value of 0
        if (version == 0) {
            while (byteBuf.readableBytes() > 0) {
                ByteBuf versionBytBuf = byteBuf.readBytes(4);
                logger.info("supported version hexdump: {}", ByteBufUtil.hexDump(versionBytBuf));
            }
        }
        return false;
    }
}
