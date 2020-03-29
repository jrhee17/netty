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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol.AddressFamily;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;

import java.util.List;

import static io.netty.handler.codec.haproxy.HAProxyConstants.*;

/**
 * Encodes an HAProxy proxy protocol header
 *
 * @see <a href="http://www.haproxy.org/download/1.8/doc/proxy-protocol.txt">Proxy Protocol Specification</a>
 */
public class HAProxyMessageEncoder extends MessageToByteEncoder<HAProxyMessage> {

    static final int IPv4_ADDRESS_BYTES_LENGTH = 12;
    static final int IPv6_ADDRESS_BYTES_LENGTH = 36;
    static final int UNIX_ADDRESS_BYTES_LENGTH = 216;

    @Override
    protected void encode(ChannelHandlerContext ctx, HAProxyMessage msg, ByteBuf out) throws Exception {
        if (msg.protocolVersion() == HAProxyProtocolVersion.V1) {
            encodeV1(msg, out);
        } else {
            out.writeBytes(BINARY_PREFIX);
            out.writeByte(0x02 << 4 | msg.command().byteValue());
            out.writeByte(msg.proxiedProtocol().byteValue());

            int tlvSize = msg.tlvSize();

            List<HAProxyTLV> tlvs = msg.tlvs();
            if (msg.proxiedProtocol().addressFamily() == AddressFamily.AF_IPv4) {
                out.writeShort(IPv4_ADDRESS_BYTES_LENGTH + tlvSize);
                out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.sourceAddress()));
                out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.destinationAddress()));
                out.writeShort(msg.sourcePort());
                out.writeShort(msg.destinationPort());
                encodeTlvs(tlvs, out);
            } else if (msg.proxiedProtocol().addressFamily() == AddressFamily.AF_IPv6) {
                out.writeShort(IPv6_ADDRESS_BYTES_LENGTH + tlvSize);
                out.writeBytes(NetUtil.getByName(msg.sourceAddress()).getAddress());
                out.writeBytes(NetUtil.getByName(msg.destinationAddress()).getAddress());
                out.writeShort(msg.sourcePort());
                out.writeShort(msg.destinationPort());
                encodeTlvs(tlvs, out);
            } else if (msg.proxiedProtocol().addressFamily() == AddressFamily.AF_UNIX) {
                out.writeShort(UNIX_ADDRESS_BYTES_LENGTH + tlvSize);
                byte[] srcAddressBytes = msg.sourceAddress().getBytes(CharsetUtil.US_ASCII);
                out.writeBytes(srcAddressBytes);
                out.writeBytes(new byte[108 - srcAddressBytes.length]);
                byte[] dstAddressBytes = msg.destinationAddress().getBytes(CharsetUtil.US_ASCII);
                out.writeBytes(dstAddressBytes);
                out.writeBytes(new byte[108 - dstAddressBytes.length]);
                encodeTlvs(tlvs, out);
            } else if (msg.proxiedProtocol().addressFamily() == AddressFamily.AF_UNSPEC) {
                out.writeShort(0);
            }
        }
    }

    void encodeV1(HAProxyMessage msg, ByteBuf out) {
        final String protocol = msg.proxiedProtocol().name();
        StringBuilder sb = new StringBuilder(108)
                .append("PROXY ").append(protocol).append(' ')
                .append(msg.sourceAddress()).append(' ')
                .append(msg.destinationAddress()).append(' ')
                .append(msg.sourcePort()).append(' ').append(msg.destinationPort()).append("\r\n");
        out.writeBytes(sb.toString().getBytes(CharsetUtil.US_ASCII));
    }

    static void encodeTlv(HAProxyTLV haProxyTLV, ByteBuf out) {
        if (haProxyTLV instanceof HAProxySSLTLV) {
            HAProxySSLTLV ssltlv = (HAProxySSLTLV) haProxyTLV;
            out.writeByte(haProxyTLV.typeByteValue());
            out.writeShort(ssltlv.contentSize());
            out.writeByte(ssltlv.client());
            out.writeInt(ssltlv.verify());
            encodeTlvs(ssltlv.encapsulatedTLVs(), out);
        } else {
            out.writeByte(haProxyTLV.typeByteValue());
            ByteBuf value = haProxyTLV.content();
            int readableBytes = value.readableBytes();
            out.writeShort(readableBytes);
            out.writeBytes(value.readSlice(readableBytes));
        }
    }

    static void encodeTlvs(List<HAProxyTLV> haProxyTLVs, ByteBuf out) {
        for (HAProxyTLV tlv: haProxyTLVs) {
            encodeTlv(tlv, out);
        }
    }
}
