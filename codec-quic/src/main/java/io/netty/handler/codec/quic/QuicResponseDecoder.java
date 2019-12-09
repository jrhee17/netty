package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

public class QuicResponseDecoder extends QuicObjectDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        final ByteBuf byteBuf = msg.content();

        if (parseLongPacketHeader(byteBuf)) {
            return;
        }

        out.add(new QuicMessage(ReferenceCountUtil.retain(byteBuf)));
    }
}
