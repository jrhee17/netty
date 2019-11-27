package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import java.nio.ByteBuffer;

public class BasicSslTest {
    @Test
    public void sandboxSslTest() throws Exception {
        final SslContext sslContext = SslContextBuilder.forClient().protocols("TLSv1.2").build();
        final SSLEngine sslEngine = sslContext.newEngine(UnpooledByteBufAllocator.DEFAULT);

        final int appBufferSize = sslEngine.getSession().getApplicationBufferSize();
        ByteBuffer src = ByteBuffer.allocate(10);
        ByteBuffer dst = ByteBuffer.allocate(appBufferSize * 2);
        SSLEngineResult result = sslEngine.wrap(src, dst);

        System.out.println(ByteBufUtil.prettyHexDump(Unpooled.copiedBuffer(dst)));;
        System.out.println(dst.remaining());
        System.out.println(result);
    }
}
