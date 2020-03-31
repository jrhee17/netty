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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class HAProxyHandler extends ProxyHandler {

    protected HAProxyHandler(SocketAddress proxyAddress) {
        super(proxyAddress);
    }

    @Override
    public String protocol() {
        return "haproxy";
    }

    @Override
    public String authScheme() {
        return AUTH_NONE;
    }

    @Override
    protected void addCodec(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().addBefore(ctx.name(), null, new HAProxyMessageEncoder());
    }

    @Override
    protected void removeEncoder(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().remove(HAProxyMessageEncoder.class);
    }

    @Override
    protected void removeDecoder(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    protected Object newInitialMessage(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = proxyAddress();
        InetAddress address = socketAddress.getAddress();

        InetSocketAddress destAddress = destinationAddress();
        InetAddress dest = destAddress.getAddress();
        return new HAProxyMessage(
                HAProxyProtocolVersion.V1, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP6,
                address.getHostAddress(), dest.getHostAddress(), socketAddress.getPort(), destAddress.getPort());
    }

    @Override
    protected boolean handleResponse(ChannelHandlerContext ctx, Object response) throws Exception {
        ByteBuf res = (ByteBuf) response;
        ctx.fireChannelRead(res.retain());
        return true;
    }
}
