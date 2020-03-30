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

import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;

public class HAProxyHandler extends ProxyHandler {

    protected HAProxyHandler(SocketAddress proxyAddress) {
        super(proxyAddress);
    }

    @Override
    public String protocol() {
        return null;
    }

    @Override
    public String authScheme() {
        return null;
    }

    @Override
    protected void addCodec(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    protected void removeEncoder(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    protected void removeDecoder(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    protected Object newInitialMessage(ChannelHandlerContext ctx) throws Exception {
        return null;
    }

    @Override
    protected boolean handleResponse(ChannelHandlerContext ctx, Object response) throws Exception {
        return false;
    }
}
