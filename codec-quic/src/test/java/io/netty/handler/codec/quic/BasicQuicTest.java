package io.netty.handler.codec.quic;

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
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;


public class BasicQuicTest {

    private static final String ECHO_MESSAGE = "hai:)";
    private static final String CHROMIUM_ENDPOINT = "https://quic.rocks:4433/";
    private static final String SAMPLE_INITIAL_HEXDUMP_PACKET = "cfff00001810b57f88e894e16fda41d0f29f49677f751477bc1a6f2950e4a601dd16df156bb93535e08711004482f6fff9222619c5ff07a85338139476ccda979adbc2f04e90fbbf8f8266af3005306fbba8b0f7dc6b89e0a4485ccfeaa8fe2d6c59bc0e1b376f6f02c5bc8ac90404bd7d2b3accd2c11e519092042957e9863a9ce927b7176f7536c46489ca7710b6e485b53edc43f44f5108f6a52d783a64d24857ba2ce24b5eeafe39cf02e8547fdb814e67d5c3c60045ce6c6a4f0113c895502c63f5c52c4c6dea34c91cc0691f6764507fc0f63af13a59728b063ada930234ba9b05f5f7d4add36b15028a2d892a55c798e96bbdc3bb91490cde96c9f37d336baa287584cc93101d5284ac340bbd29c3548c098532446e0bb95508fb878c33bc21a34273d15552a0fd450da2748700550ea33c187d0fd7bb0a94b57eb0803be430483118e8fe98fbeb8608436d8f39abe30c28fba360fc4675e401d64c1bd87bdfb647422a05432d7aaeeb600c85bbfbb99bda07deb2a5faf17f8c1b5de461fb6c41faa20338e54531b3080893af30c8aab269fef41051772df2b0a05ba393505b29c8b9edfd3085f359a64c2c8749aeb82e1ac7d0f3234af47b7ec9a51dbb81debbfc72490aad40c5377298d8cf0b21bf8f8750dfe3b45833f6a425d9cd8daaeb1aac1c44e69f198b5a9015364177bcba7f5022b6fdaf123de110360b0dd619a37dc96cbe2809df1e481a7e3b500911cb49c1bfe8dca4c6230ab816f187875a4c70198534f4e3e77ec76341f00135d3d7b701f3a1f2e6743b45d3af9a00fe4938fba2ab30cb3d3c67d0a237ab0be45e1229b72280bdec7e61c01da2329967a21ced88adc79e73818eb8aa583591c85f34dfe1231b70c5769cd8945bd642b607453c853b2061fd259b57e09743e54d6cd7b6344a1e9a817b94e2272f797a0a171ac14de318986bdf723eab648f3deeb4e18211f37e79a014e7b39ff07bf84b353936de53868f70a7a3d1be70186e741eeff84461afe9905a85ea18f0ec3586f332f3ef2db99921be70f2b89450b7e45b2ae381d4ef4e32abbfd5c11736da4c871bc7eacab8a5b7238925fd3cb8acd824de004b55bdc78bc6f518666772cd945444101d27f5141926246d7e8d95009ed2763d0bf88411f5eace577850d04d15e172116fc1fcd7d246d0a837627b6e7a610365e343d7544dd46c799daf74c05ed9c1a6b71c3598b9b4f7461f45cb88c6a6005269b9f8b1b3aefad2e7c3a12831d428337699fc414bde0320dc334ba06ac88c3522f8636b50c4bc15a1d088757c62882d5495b06d4d83fee1ecff34ab20f5e7003aae70a962d7ded730fc5ee75cb2fb8601429bec3211a70c194ba903bb38e1f182612dedfad96702c6cf55046fe04f89a3534d4006f288ffbd38d89dbbbd9d37ab5c188ee45264410f9fea51d393710b1ae834a80d91edda4aa63f60bfa582bb4451408f30b1e4dbef075b6f6554b7ac31ed7520158ee2fe7bff6a7038762826c6c40553f5b567f06fc75b3f387e9ddf3f1486348158868de7166679442cdee500ef61fb76dcf80d5a7c819831276e86d8736cdb21ee2aa22e0ca55c83bafc56e74be1ad935150653f6c34dd39bb67ab2357a873f0428df56d1c6f9a583c5feac8cfa71da32b8527d0de7baf725959b862b915bd091a7ea9ab6e4e72";

    @Test
    public void testSimpleEcho() throws Throwable {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>();

        final Channel serverChannel = setupServer(retval -> {
            result.set(retval);
            latch.countDown();
        });

        final InetSocketAddress recipient = new InetSocketAddress(20080);
        final Channel clientChannel = getClient(recipient, ignored -> {});
        clientChannel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(
                ECHO_MESSAGE.getBytes()), recipient)).sync();

        latch.await();

        assertEquals(ECHO_MESSAGE, result.get());

        serverChannel.close().await();
        clientChannel.close().await();
    }

    @Test
    public void trySendInitialPacket() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ByteBuf> result = new AtomicReference<>();

        final InetSocketAddress remote = new InetSocketAddress("quic.tech", 4433);
        final Channel client = getClient(remote, res -> {
            result.set(Unpooled.copiedBuffer(res));
            latch.countDown();
        });

//        final QuicFrame quicFrame = new QuicFrame((byte) 0x06, new byte[]{0}, new byte[] {0}, new byte[]{0});
//        client.writeAndFlush(new QuicRequest(remote, 1, 1, 0, 0,
//                                             0x00000001, new byte[] {0}, new byte[] {0}, 0, new byte[] {0},
//                                             quicFrame)).sync();
        client.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(ByteBufUtil.decodeHexDump(
                SAMPLE_INITIAL_HEXDUMP_PACKET)), remote)).sync();

        latch.await();
        assertNotNull(result.get());

        client.close().await();
    }

    @Test
    public void initialPacket() {
        final byte[] bytes = ByteBufUtil.decodeHexDump("07ff706cb107568ef7116f5f58a9ed9010");
        System.out.println(ByteBufUtil.prettyHexDump(Unpooled.copiedBuffer(bytes)));
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

                     @Override
                     public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                         System.out.println(cause);
                     }
                 });
             }
         });
        return b.bind(new InetSocketAddress(20080)).sync().await().channel();
    }

    public static Channel getClient(InetSocketAddress address, Consumer<ByteBuf> validator) throws Exception {
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
                         p.addLast(new QuicResponseDecoder());
                         p.addLast(new QuicRequestEncoder());
                         p.addLast(new SimpleChannelInboundHandler<QuicMessage>() {
                             @Override
                             protected void messageReceived(ChannelHandlerContext ctx, QuicMessage msg) throws Exception {
                                 final ByteBuf buf = msg.getByteBuf();
                                 validator.accept(buf);
                             }
                         });
                     }
                 });
        final ChannelFuture channelFuture = bootstrap.connect(address).sync();
        return channelFuture.channel();
    }
}
