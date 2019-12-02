package io.netty.handler.codec.quic;

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

    private final SelfSignedCertificate cert = new SelfSignedCertificate();
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    public BasicSslTest() throws CertificateException {
    }

    private SSLEngine clientSslEngine() throws SSLException {
        if (clientSslEngine == null) {
            synchronized (this) {
                if (clientSslEngine == null) {
                    final SslContext sslContext = SslContextBuilder.forClient().trustManager(cert.cert()).protocols(
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
                            cert.certificate(), cert.privateKey()).protocols("TLSv1", "TLSv1.1", "TLSv1.2").build();
                    serverSslEngine = sslContext.newEngine(UnpooledByteBufAllocator.DEFAULT);
                }
            }
        }
        return serverSslEngine;
    }

    @Test
    public void sandboxSslTest() throws Exception {

        ByteBuffer clientPacketBuf = ByteBuffer.allocate(clientSslEngine().getSession().getPacketBufferSize());
        ByteBuffer serverPacketBuf = ByteBuffer.allocate(serverSslEngine().getSession().getPacketBufferSize());

        ByteBuffer serverAppBuf = ByteBuffer.allocate(
                serverSslEngine().getSession().getApplicationBufferSize());
        ByteBuffer clientAppBuf = ByteBuffer.allocate(
                clientSslEngine().getSession().getApplicationBufferSize());

        // Start client side handshaking
        assert clientSslEngine().getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING;

        clientSslEngine().beginHandshake();

        assert clientSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        SSLEngineResult result = clientSslEngine().wrap(EMPTY_BUFFER, clientPacketBuf);

        assert result.getStatus() == Status.OK;
        assert result.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING;

        serverSslEngine().beginHandshake();

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;

        clientPacketBuf.flip();
        SSLEngineResult serverResult1 = serverSslEngine().unwrap(clientPacketBuf, serverAppBuf);
        clientPacketBuf.compact();

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_TASK;
        assert serverResult1.getStatus() == Status.OK;

        Runnable task;
        while ((task = serverSslEngine().getDelegatedTask()) != null) {
            task.run();
        }

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        SSLEngineResult serverResult2 = serverSslEngine().wrap(EMPTY_BUFFER, serverPacketBuf);

        assert serverResult2.getStatus() == Status.OK;

        serverPacketBuf.flip();
        SSLEngineResult clientResult2 = clientSslEngine().unwrap(serverPacketBuf, clientAppBuf);
        serverPacketBuf.compact();

        assert clientResult2.getHandshakeStatus() == HandshakeStatus.NEED_TASK;
        assert clientResult2.getStatus() == Status.OK;

        while ((task = clientSslEngine().getDelegatedTask()) != null) {
            task.run();
        }

        assert clientSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        SSLEngineResult clientResult3 = clientSslEngine().wrap(EMPTY_BUFFER, clientPacketBuf);

        assert clientResult3.getStatus() == Status.OK;
        assert clientResult3.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        clientPacketBuf.flip();
        SSLEngineResult serverResult3 = serverSslEngine().unwrap(clientPacketBuf, serverAppBuf);
        clientPacketBuf.compact();

        assert serverResult3.getStatus() == Status.OK;
        assert serverResult3.getHandshakeStatus() == HandshakeStatus.NEED_TASK;

        while ((task = serverSslEngine().getDelegatedTask()) != null) {
            task.run();
        }

        assert serverSslEngine().getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;

        SSLEngineResult clientResult4 = clientSslEngine().wrap(EMPTY_BUFFER, clientPacketBuf);

        assert clientResult4.getStatus() == Status.OK;
        assert clientResult4.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        clientPacketBuf.flip();
        SSLEngineResult serverResult4 = serverSslEngine().unwrap(clientPacketBuf, serverAppBuf);
        clientPacketBuf.compact();

        assert serverResult4.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;
        assert serverResult4.getStatus() == Status.OK;

        SSLEngineResult clientResult5 = clientSslEngine().wrap(EMPTY_BUFFER, clientPacketBuf);

        assert clientResult5.getStatus() == Status.OK;
        assert clientResult5.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;

        clientPacketBuf.flip();
        SSLEngineResult serverResult5 = serverSslEngine().unwrap(clientPacketBuf, serverAppBuf);
        clientPacketBuf.compact();

        assert serverResult5.getStatus() == Status.OK;
        assert serverResult5.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;

        SSLEngineResult serverResult6 = serverSslEngine().wrap(EMPTY_BUFFER, serverPacketBuf);

        assert serverResult6.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;
        assert serverResult6.getStatus() == Status.OK;

        serverPacketBuf.flip();
        SSLEngineResult clientResult6 = clientSslEngine().unwrap(serverPacketBuf, clientAppBuf);
        serverPacketBuf.compact();

        assert clientResult6.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP;
        assert clientResult6.getStatus() == Status.OK;

        SSLEngineResult serverResult7 = serverSslEngine().wrap(EMPTY_BUFFER, serverPacketBuf);

        assert serverResult7.getHandshakeStatus() == HandshakeStatus.FINISHED;
        assert serverResult7.getStatus() == Status.OK;

        serverPacketBuf.flip();
        SSLEngineResult clientResult7 = clientSslEngine().unwrap(serverPacketBuf, clientAppBuf);
        serverPacketBuf.compact();

        assert clientResult7.getStatus() == Status.OK;
        assert clientResult7.getHandshakeStatus() == HandshakeStatus.FINISHED;
    }
}
