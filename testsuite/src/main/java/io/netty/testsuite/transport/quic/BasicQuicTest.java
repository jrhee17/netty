package io.netty.testsuite.transport.quic;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
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

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class BasicQuicTest {
    @Test
    public void testSimpleEcho() throws Throwable {
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
                         final InetAddress srcAddr = msg.sender().getAddress();
                         final ByteBuf buf = msg.content();
                         final int rcvPktLength = buf.readableBytes();
                         final byte[] rcvPktBuf = new byte[rcvPktLength];
                         buf.readBytes(rcvPktBuf);
                         System.out.println("Inside incomming packet handler");

                     }
                 });
             }
         });

        // Bind and start to accept incoming connections.
        Channel channel = b.bind(new InetSocketAddress(20080)).sync().channel();
        channel.closeFuture().await();
    }

    @Test
    public void testSimpleClient() throws Exception {
        final Bootstrap bootstrap = new Bootstrap();
        final MultithreadEventLoopGroup workerGroup = new MultithreadEventLoopGroup(NioHandler.newFactory());
        bootstrap.group(workerGroup)
                 .channel(NioDatagramChannel.class)
                 .option(ChannelOption.SO_BROADCAST, true)
                 .handler(new ChannelInitializer<NioDatagramChannel>() {
                     @Override
                     protected void initChannel(NioDatagramChannel ch)throws Exception {
                         final ChannelPipeline pipeline = ch.pipeline();
                         pipeline.addLast(new ChannelHandler() {
                             @Override
                             public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                 ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("hai:)".getBytes()), new InetSocketAddress(20080)));
                             }

                             @Override
                             public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                 System.out.println("msg: " + msg);
                             }
                         });
                     }
                 });
        final ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(20080)).sync();
        System.out.println(channelFuture.channel().remoteAddress());
        channelFuture.channel().closeFuture().await(1000);
    }
}
