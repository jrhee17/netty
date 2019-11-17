package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

public class QuicRequest extends QuicMessage {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicRequest.class);

    private final InetSocketAddress inetSocketAddress;
    private final int headerForm;
    private final int fixedBit;
    private final int longPacketType;
    private final int typeSpecificBits;
    private final int version;
    private final byte[] dcid;
    private final byte[] scid;
    private final int tokenLength;
    private final byte[] packetNumber;

    public QuicRequest(InetSocketAddress inetSocketAddress, int headerForm, int fixedBit,
                       int longPacketType, int typeSpecificBits, int version, byte[] dcid, byte[] scid, int tokenLength,
                       byte[] packetNumber) {
        this.inetSocketAddress = inetSocketAddress;
        this.headerForm = headerForm;
        this.fixedBit = fixedBit;
        this.longPacketType = longPacketType;
        this.typeSpecificBits = typeSpecificBits;
        this.version = version;
        this.dcid = dcid.clone();
        this.scid = scid.clone();
        this.tokenLength = tokenLength;
        this.packetNumber = packetNumber.clone();
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    @Override
    public ByteBuf getByteBuf() {
        byte header = (byte) (((headerForm & 0x01) << 7) + ((fixedBit & 0x01) << 6) + ((longPacketType & 0x03) << 5) + (typeSpecificBits & 0x0f));
        System.out.println(header);
        return Unpooled.buffer()
                       .writeByte(header)
                       .writeInt(version)
                       .writeByte(dcid.length - 1)
                       .writeBytes(dcid)
                       .writeByte(scid.length - 1)
                       .writeBytes(scid)
                       .writeByte(tokenLength)
                       .writeBytes(variableLengthIntegerEncoding(packetNumber.length))
                       .writeBytes(packetNumber);
    }

    static byte[] variableLengthIntegerEncoding(long length) {
        if (length < 64) {
            return new byte[] { (byte) (length & 0xff) };
        } else if (length < 16384) {
            return new byte[] { (byte) (((length & 0xff00) >> 8) + 0x40), (byte) (length & 0xff) };
        } else if (length < 1073741823) {
            return new byte[] {
                    (byte) (((length & 0xff000000) >> 24) + 0x80),
                    (byte) ((length & 0xff0000) >> 16),
                    (byte) ((length & 0xff00) >> 8),
                    (byte) (length & 0xff),
            };
        } else if (length < 4611686018427387904L) {
            return new byte[] {
                    (byte) (((length & 0xff00000000000000L) >> 56) + 0xc0),
                    (byte) ((length & 0xff000000000000L) >> 48),
                    (byte) ((length & 0xff0000000000L) >> 40),
                    (byte) ((length & 0xff00000000L) >> 32),
                    (byte) ((length & 0xff000000) >> 24),
                    (byte) ((length & 0xff0000) >> 16),
                    (byte) ((length & 0xff00) >> 8),
                    (byte) (length & 0xff),
            };
        } else {
            throw new IllegalArgumentException("invalid length: " + length);
        }
    }
}
