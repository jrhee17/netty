package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.InetSocketAddress;

public class QuicRequest extends QuicMessage {

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
                       .writeBytes(packetNumber);
    }
}
