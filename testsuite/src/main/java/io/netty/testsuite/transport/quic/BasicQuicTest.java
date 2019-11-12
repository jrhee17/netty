package io.netty.testsuite.transport.quic;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;


public class BasicQuicTest {

    private static final String ECHO_MESSAGE = "hai:)";
    private static final String CHROMIUM_ENDPOINT = "https://quic.rocks:4433/";

    @Test
    public void testSimpleEcho() throws Throwable {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>();

        final Channel serverChannel = setupServer(retval -> {
            result.set(retval);
            latch.countDown();
        });

        final Channel clientChannel = getClient();
        clientChannel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(
                ECHO_MESSAGE.getBytes()), new InetSocketAddress(20080))).sync();

        latch.await();

        assertEquals(ECHO_MESSAGE, result.get());

        serverChannel.close().await();
        clientChannel.close().await();
    }

    private static Channel setupServer(Consumer<String> validator) throws Exception {
        final MultithreadEventLoopGroup group = new MultithreadEventLoopGroup(NioHandler.newFactory());
        final Bootstrap b = new Bootstrap();
        b.group(group).channel(NioDatagramChannel.class)
         .option(ChannelOption.SO_BROADCAST, true)
         .handler(new ChannelInitializer<NioDatagramChannel>() {
             @Override
             public void initChannel(final NioDatagramChannel ch) throws Exception {

                 final ChannelPipeline p = ch.pipeline();
                 p.addLast(new LoggingHandler());
                 p.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                     @Override
                     protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                         final ByteBuf buf = msg.content();
                         final int rcvPktLength = buf.readableBytes();
                         final byte[] rcvPktBuf = new byte[rcvPktLength];
                         buf.readBytes(rcvPktBuf);
                         validator.accept(new String(rcvPktBuf));
                     }
                 });
             }
         });
        return b.bind(new InetSocketAddress(20080)).sync().await().channel();
    }

    public static Channel getClient() throws Exception {
        final Bootstrap bootstrap = new Bootstrap();
        final MultithreadEventLoopGroup workerGroup = new MultithreadEventLoopGroup(NioHandler.newFactory());
        bootstrap.group(workerGroup)
                 .channel(NioDatagramChannel.class)
                 .option(ChannelOption.SO_BROADCAST, true)
                 .handler(new ChannelInitializer<NioDatagramChannel>() {
                     @Override
                     protected void initChannel(NioDatagramChannel ch)throws Exception {
                     }
                 });
        final ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(20080)).sync();
        return channelFuture.channel();
    }
}
