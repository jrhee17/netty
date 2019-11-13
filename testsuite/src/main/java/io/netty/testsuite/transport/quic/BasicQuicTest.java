package io.netty.testsuite.transport.quic;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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
import io.netty.resolver.InetNameResolver;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;


public class BasicQuicTest {

    private static final String ECHO_MESSAGE = "hai:)";
    private static final String CHROMIUM_ENDPOINT = "https://quic.rocks:4433/";
    private static final InetSocketAddress remote = new InetSocketAddress("cloudflare-quic.com", 443);;

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

    @Test
    public void longHeaderPacket() throws Exception {
        byte[] headerForm = {
                (byte) ((0x80 & 0xff) + (0x40 & 0xff)),
                (byte) 0xff, 0, 0x00, 0x17,
                1, (byte) 0, 1, (byte) 0,
                0, 0, 1, 0
        };

//        final ByteBuf byteBuf = Unpooled.copyInt(headerForm);
        final ByteBuf byteBuf = Unpooled.copiedBuffer(headerForm);
        System.out.println(ByteBufUtil.prettyHexDump(byteBuf));

        final byte[] bytes = ByteBufUtil.decodeHexDump("07ff706cb107568ef7116f5f58a9ed9010");
        final ByteBuf byteBufFromInternet = Unpooled.copiedBuffer(bytes);
        System.out.println(ByteBufUtil.prettyHexDump(Unpooled.copiedBuffer(bytes)));

        final Channel client = getRemoteClient();
        client.writeAndFlush(new DatagramPacket(byteBufFromInternet, remote)).sync();
        client.closeFuture().await();
    }

    @Test
    public void initialPacket() {
        final byte[] bytes = ByteBufUtil.decodeHexDump("07ff706cb107568ef7116f5f58a9ed9010");
        System.out.println(ByteBufUtil.prettyHexDump(Unpooled.copiedBuffer(bytes)));;
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
                         final ChannelPipeline p = ch.pipeline();
                         p.addLast(new LoggingHandler());
                     }
                 });
        final ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(20080)).sync();
        return channelFuture.channel();
    }

    public static Channel getRemoteClient() throws Exception {
        final Bootstrap bootstrap = new Bootstrap();
        final MultithreadEventLoopGroup workerGroup = new MultithreadEventLoopGroup(NioHandler.newFactory());
        bootstrap.group(workerGroup)
                 .channel(NioDatagramChannel.class)
                 .option(ChannelOption.SO_BROADCAST, true)
                 .handler(new ChannelInitializer<NioDatagramChannel>() {
                     @Override
                     protected void initChannel(NioDatagramChannel ch)throws Exception {
                         final ChannelPipeline p = ch.pipeline();
                         p.addLast(new LoggingHandler());
                         p.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                             @Override
                             protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                                 System.out.println(msg);
                             }
                         });
                     }
                 });
        final ChannelFuture channelFuture = bootstrap.connect(remote).sync();
        return channelFuture.channel();
    }
}
