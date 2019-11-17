package io.netty.handler.codec.quic;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class QuicRequestEncoder extends MessageToMessageEncoder<QuicRequest> {
    @Override
    protected void encode(ChannelHandlerContext ctx, QuicRequest msg, List<Object> out) throws Exception {
        out.add(new DatagramPacket(msg.getByteBuf(), msg.getInetSocketAddress()));
    }
}
