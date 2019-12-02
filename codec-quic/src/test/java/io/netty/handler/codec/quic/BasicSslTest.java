package io.netty.handler.codec.quic;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;

public class BasicSslTest {

    private volatile SSLEngine clientSslEngine;
    private volatile SSLEngine serverSslEngine;

    private final SelfSignedCertificate ssc = new SelfSignedCertificate();

    public BasicSslTest() throws CertificateException {
    }

    private SSLEngine clientSslEngine() throws SSLException {
        if (clientSslEngine == null) {
            synchronized (this) {
                if (clientSslEngine == null) {
                    final SslContext sslContext = SslContextBuilder.forClient().trustManager(ssc.cert()).protocols(
                            "TLSv1.2").build();
                    clientSslEngine = sslContext.newEngine(UnpooledByteBufAllocator.DEFAULT);
                }
            }
        }
        return clientSslEngine;
    }

    private SSLEngine serverSslEngine() throws SSLException, CertificateException {
        if (serverSslEngine == null) {
            synchronized (this) {
                if (serverSslEngine == null) {

                    final SslContext sslContext = SslContextBuilder.forServer(
                            ssc.certificate(), ssc.privateKey()).protocols("TLSv1", "TLSv1.1", "TLSv1.2").build();
                    serverSslEngine = sslContext.newEngine(UnpooledByteBufAllocator.DEFAULT);
                }
            }
        }
        return serverSslEngine;
    }

    @Test
    public void sandboxSslTest() throws Exception {

        assert clientSslEngine().getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING;

        clientSslEngine().beginHandshake();

        assert clientSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        final int packetBufferSize = clientSslEngine().getSession().getPacketBufferSize();
        ByteBuffer src = ByteBuffer.allocate(0);
        ByteBuffer dst = ByteBuffer.allocate(packetBufferSize);
        SSLEngineResult result = clientSslEngine().wrap(src, dst);

        assert result.getStatus() == Status.OK;
        assert result.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING;

        serverSslEngine().beginHandshake();

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;

        dst.flip();
        ByteBuffer dst2 = ByteBuffer.allocate(serverSslEngine().getSession().getApplicationBufferSize());
        SSLEngineResult serverResult1 = serverSslEngine().unwrap(dst, dst2);

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_TASK;
        assert serverResult1.getStatus() == Status.OK;

        Runnable task;
        while ((task = serverSslEngine().getDelegatedTask()) != null) {
            task.run();
        }

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        ByteBuffer dst3 = ByteBuffer.allocate(serverSslEngine().getSession().getPacketBufferSize());
        SSLEngineResult serverResult2 = serverSslEngine().wrap(ByteBuffer.allocate(0), dst3);

        assert serverResult2.getStatus() == Status.OK;

        ByteBuffer dst4 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        dst3.flip();
        SSLEngineResult clientResult2 = clientSslEngine().unwrap(dst3, dst4);

        assert clientResult2.getHandshakeStatus() == HandshakeStatus.NEED_TASK;
        assert clientResult2.getStatus() == Status.OK;

        while ((task = clientSslEngine().getDelegatedTask()) != null) {
            task.run();
        }

        assert clientSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        ByteBuffer dst5 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        SSLEngineResult clientResult3 = clientSslEngine().wrap(ByteBuffer.allocate(0), dst5);

        assert clientResult3.getStatus() == Status.OK;
        assert clientResult3.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        ByteBuffer dst6 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        SSLEngineResult clientResult4 = clientSslEngine().wrap(ByteBuffer.allocate(0), dst6);

        assert clientResult4.getStatus() == Status.OK;
        assert clientResult4.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        ByteBuffer dst7 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        SSLEngineResult clientResult5 = clientSslEngine().wrap(ByteBuffer.allocate(0), dst7);

        assert clientResult5.getStatus() == Status.OK;
        assert clientResult5.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;

        ByteBuffer dst8 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        dst5.flip();
        SSLEngineResult serverResult3 = serverSslEngine().unwrap(dst5, dst8);

        assert serverResult3.getStatus() == Status.OK;
        assert serverResult3.getHandshakeStatus() == HandshakeStatus.NEED_TASK;

        while ((task = serverSslEngine().getDelegatedTask()) != null) {
            task.run();
        }

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;

        ByteBuffer dst9 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        dst6.flip();
        SSLEngineResult serverResult4 = serverSslEngine().unwrap(dst6, dst9);

        assert serverResult4.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;
        assert serverResult4.getStatus() == Status.OK;

        ByteBuffer dst10 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        dst7.flip();
        SSLEngineResult serverResult5 = serverSslEngine().unwrap(dst7, dst10);

        assert serverResult5.getStatus() == Status.OK;

        ByteBuffer dst11 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        SSLEngineResult serverResult6 = serverSslEngine().wrap(ByteBuffer.allocate(0), dst11);

        assert serverResult6.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;
        assert serverResult6.getStatus() == Status.OK;

        ByteBuffer dst12 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        SSLEngineResult serverResult7 = serverSslEngine().wrap(ByteBuffer.allocate(0), dst12);

        assert serverResult7.getHandshakeStatus() == HandshakeStatus.FINISHED;
        assert serverResult7.getStatus() == Status.OK;

        ByteBuffer dst13 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        dst11.flip();
        SSLEngineResult clientResult6 = clientSslEngine().unwrap(dst11, dst13);

        assert clientResult6.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;
        assert clientResult6.getStatus() == Status.OK;

        ByteBuffer dst14 = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        dst12.flip();
        SSLEngineResult clientResult7 = clientSslEngine().unwrap(dst12, dst14);

        assert clientResult7.getStatus() == Status.OK;
        assert clientResult7.getHandshakeStatus() == HandshakeStatus.FINISHED;
    }
}
