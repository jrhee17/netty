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

package io.netty.example.haproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import io.netty.handler.codec.haproxy.HAProxySSLTLV;
import io.netty.handler.codec.haproxy.HAProxyTLV;
import io.netty.handler.codec.haproxy.HAProxyTLV.Type;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.netty.example.haproxy.HAProxyServer.*;

public final class HAProxyClient {

    private static final String HOST = System.getProperty("host", "127.0.0.1");

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new HAProxyMessageEncoder());

            // Start the connection attempt.
            Channel ch = b.connect(HOST, PORT).sync().channel();

            HAProxyTLV alpnTlv = new HAProxyTLV(Type.PP2_TYPE_ALPN, Unpooled.EMPTY_BUFFER);
            HAProxyTLV authorityTlv = new HAProxyTLV(
                    Type.PP2_TYPE_AUTHORITY, Unpooled.copiedBuffer("authority".getBytes(CharsetUtil.US_ASCII)));
            List<HAProxyTLV> tlvs = new ArrayList<HAProxyTLV>();
            tlvs.add(alpnTlv);
            tlvs.add(authorityTlv);
            HAProxySSLTLV sslTlv = new HAProxySSLTLV(1, (byte) 0x01, tlvs);

            HAProxyMessage message = new HAProxyMessage(
                    HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4,
                    "127.0.0.1", "127.0.0.2", 8000, 9000,
                    Collections.singletonList(sslTlv));
            ch.writeAndFlush(message).sync();
            ch.writeAndFlush(Unpooled.copiedBuffer("Hello World!", CharsetUtil.US_ASCII)).sync();
            ch.writeAndFlush(Unpooled.copiedBuffer("Bye now!", CharsetUtil.US_ASCII)).sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
