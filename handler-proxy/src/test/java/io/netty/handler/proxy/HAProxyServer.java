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

package io.netty.handler.proxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class HAProxyServer extends ProxyServer {
    protected HAProxyServer(boolean useSsl, TestMode testMode, InetSocketAddress destination) {
        super(useSsl, testMode, destination);
    }

    @Override
    protected void configure(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        switch (testMode) {
        case INTERMEDIARY:
            p.addLast(new HAProxyMessageDecoder());
            p.addLast(new HAProxyIntermediaryHandler());
            break;
        case TERMINAL:
            p.addLast("lineDecoder", new LineBasedFrameDecoder(128, false, true));
            p.addLast(new HAProxyMessageDecoder());
            p.addLast(new HAProxyTerminalHandler());
            break;
        case UNRESPONSIVE:
            p.addLast(UnresponsiveHandler.INSTANCE);
            break;
        }
    }

    private final class HAProxyIntermediaryHandler extends IntermediaryHandler {

        private SocketAddress intermediaryDestination;

        @Override
        protected boolean handleProxyProtocol(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof HAProxyMessage)) {
                return false;
            }

            HAProxyMessage proxyMessage = (HAProxyMessage) msg;
            intermediaryDestination = new InetSocketAddress(
                    proxyMessage.destinationAddress(), proxyMessage.destinationPort());

            ctx.pipeline().remove(HAProxyMessageDecoder.class);

            return true;
        }

        @Override
        protected SocketAddress intermediaryDestination() {
            return intermediaryDestination;
        }
    }

    private final class HAProxyTerminalHandler extends TerminalHandler {
        @Override
        protected boolean handleProxyProtocol(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof HAProxyMessage)) {
                return false;
            }

            HAProxyMessage proxyMessage = (HAProxyMessage) msg;
            if (!proxyMessage.destinationAddress().equals(destination.getHostString()) ||
                proxyMessage.destinationPort() != destination.getPort()) {
                ctx.close();
                return false;
            }
            ctx.write(Unpooled.copiedBuffer("0\n", CharsetUtil.US_ASCII));
            return true;
        }
    }
}
