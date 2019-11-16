package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class QuicResponseDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(QuicResponseDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        final ByteBuf byteBuf = msg.content();
        logger.info("decoding byteBuf: {}", byteBuf);
        out.add(new QuicMessage(ReferenceCountUtil.retain(byteBuf)));
    }
}
