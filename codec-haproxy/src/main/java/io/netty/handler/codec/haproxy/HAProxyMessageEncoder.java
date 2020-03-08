/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.haproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol.AddressFamily;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;

import java.util.List;

/**
 * Encodes an HAProxy proxy protocol header
 *
 * @see <a href="http://www.haproxy.org/download/1.8/doc/proxy-protocol.txt">Proxy Protocol Specification</a>
 */
public class HAProxyMessageEncoder extends MessageToByteEncoder<HAProxyMessage> {

    /**
     * Binary header prefix
     */
    static final byte[] BINARY_PREFIX = {
            (byte) 0x0D,
            (byte) 0x0A,
            (byte) 0x0D,
            (byte) 0x0A,
            (byte) 0x00,
            (byte) 0x0D,
            (byte) 0x0A,
            (byte) 0x51,
            (byte) 0x55,
            (byte) 0x49,
            (byte) 0x54,
            (byte) 0x0A
    };

    static final int V2_HEADER_BYTES_LENGTH = 16;
    static final int IPv4_ADDRESS_BYTES_LENGTH = 12;
    static final int IPv6_ADDRESS_BYTES_LENGTH = 36;
    static final int UNIX_ADDRESS_BYTES_LENGTH = 216;

    @Override
    protected void encode(ChannelHandlerContext ctx, HAProxyMessage msg, ByteBuf out) throws Exception {
        if (msg.protocolVersion() == HAProxyProtocolVersion.V1) {
            out.writeBytes(encoderHeader(msg).getBytes(CharsetUtil.US_ASCII));
        }
        else {
            out.writeBytes(BINARY_PREFIX);
            out.writeByte(0x02 << 4 | msg.command().byteValue());
            out.writeByte(msg.proxiedProtocol().byteValue());
            List<HAProxyTLV> tlvs = msg.tlvs();
            ByteBuf encodedTlvs = encodeTlvs(tlvs);
            if (msg.proxiedProtocol().addressFamily() == AddressFamily.AF_IPv4) {
                out.writeShort(IPv4_ADDRESS_BYTES_LENGTH + encodedTlvs.readableBytes());
                out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.sourceAddress()));
                out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.destinationAddress()));
                out.writeShort(msg.sourcePort());
                out.writeShort(msg.destinationPort());
                out.writeBytes(encodedTlvs);
            } else if (msg.proxiedProtocol().addressFamily() == AddressFamily.AF_IPv6) {
                out.writeShort(IPv6_ADDRESS_BYTES_LENGTH + encodedTlvs.readableBytes());
                out.writeBytes(NetUtil.getByName(msg.sourceAddress()).getAddress());
                out.writeBytes(NetUtil.getByName(msg.destinationAddress()).getAddress());
                out.writeShort(msg.sourcePort());
                out.writeShort(msg.destinationPort());
                out.writeBytes(encodedTlvs);
            } else if (msg.proxiedProtocol().addressFamily() == AddressFamily.AF_UNIX) {
                out.writeShort(UNIX_ADDRESS_BYTES_LENGTH + encodedTlvs.readableBytes());
                byte[] srcAddressBytes = msg.sourceAddress().getBytes(CharsetUtil.US_ASCII);
                out.writeBytes(srcAddressBytes);
                out.writeBytes(new byte[108 - srcAddressBytes.length]);
                byte[] dstAddressBytes = msg.destinationAddress().getBytes(CharsetUtil.US_ASCII);
                out.writeBytes(dstAddressBytes);
                out.writeBytes(new byte[108 - dstAddressBytes.length]);
                out.writeBytes(encodedTlvs);
            } else if (msg.proxiedProtocol().addressFamily() == AddressFamily.AF_UNSPEC) {
                out.writeShort(0);
            }
        }
    }

    String encoderHeader(HAProxyMessage msg) {
        final String protocol = msg.proxiedProtocol().name();
        return "PROXY " + protocol + ' ' + msg.sourceAddress() + ' ' + msg.destinationAddress() +
               ' ' + msg.sourcePort() + ' ' + msg.destinationPort() + "\r\n";
    }

    ByteBuf encodeTlv(HAProxyTLV haProxyTLV) {
        if (haProxyTLV instanceof HAProxySSLTLV) {
            HAProxySSLTLV ssltlv = (HAProxySSLTLV) haProxyTLV;
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeByte(haProxyTLV.typeByteValue());

            byte client = ssltlv.client();
            int verify = ssltlv.verify();
            ByteBuf innerTlvBuf = encodeTlvs(ssltlv.encapsulatedTLVs());

            buffer.writeShort(1 + 4 + innerTlvBuf.readableBytes());
            buffer.writeByte(client);
            buffer.writeInt(verify);
            buffer.writeBytes(innerTlvBuf);

            return buffer;
        } else {
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeByte(haProxyTLV.typeByteValue());
            ByteBuf value = haProxyTLV.content();
            int readableBytes = value.readableBytes();
            buffer.writeShort(readableBytes);
            buffer.writeBytes(value.readSlice(readableBytes));
            return buffer;
        }
    }

    ByteBuf encodeTlvs(List<HAProxyTLV> haProxyTLVs) {
        ByteBuf byteBuf = Unpooled.buffer();
        for (HAProxyTLV tlv: haProxyTLVs) {
            byteBuf.writeBytes(encodeTlv(tlv));
        }
        return byteBuf;
    }
}
