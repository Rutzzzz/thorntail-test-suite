package org.wildfly.swarm.ts.javaee8.servlet;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpConnection;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.Methods;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncRequester;
import org.apache.hc.core5.http.nio.AsyncClientEndpoint;
import org.apache.hc.core5.http.nio.entity.StringAsyncEntityConsumer;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.nio.support.BasicResponseConsumer;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.http2.frame.RawFrame;
import org.apache.hc.core5.http2.impl.nio.H2StreamListener;
import org.apache.hc.core5.http2.impl.nio.bootstrap.H2RequesterBootstrap;
import org.apache.hc.core5.http2.ssl.ConscryptClientTlsStrategy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.conscrypt.Conscrypt;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;


@RunWith(Arquillian.class)
@DefaultDeployment
public class PushServletTest {

    @RunAsClient
    @Test
    public void PushServletTest() throws KeyManagementException, NoSuchAlgorithmException, InterruptedException, ExecutionException {
        // Create and start requester
        final H2Config h2Config = H2Config.custom()
                .setPushEnabled(false)
                .build();

        final SSLContext sslContext = SSLContexts.custom()
                .setProvider(Conscrypt.newProvider())
                .build();

        final HttpAsyncRequester requester = H2RequesterBootstrap.bootstrap()
                .setH2Config(h2Config)
                .setTlsStrategy(new ConscryptClientTlsStrategy(sslContext))
                .setStreamListener(new H2StreamListener() {

                    @Override
                    public void onHeaderInput(final HttpConnection connection, final int streamId, final List<? extends Header> headers) {
                        for (int i = 0; i < headers.size(); i++) {
                            System.out.println(connection.getRemoteAddress() + " (" + streamId + ") << " + headers.get(i));
                        }
                    }

                    @Override
                    public void onHeaderOutput(final HttpConnection connection, final int streamId, final List<? extends Header> headers) {
                        for (int i = 0; i < headers.size(); i++) {
                            System.out.println(connection.getRemoteAddress() + " (" + streamId + ") >> " + headers.get(i));
                        }
                    }

                    @Override
                    public void onFrameInput(final HttpConnection connection, final int streamId, final RawFrame frame) {
                    }

                    @Override
                    public void onFrameOutput(final HttpConnection connection, final int streamId, final RawFrame frame) {
                    }

                    @Override
                    public void onInputFlowControl(final HttpConnection connection, final int streamId, final int delta, final int actualSize) {
                    }

                    @Override
                    public void onOutputFlowControl(final HttpConnection connection, final int streamId, final int delta, final int actualSize) {
                    }

                })
                .create();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("HTTP requester shutting down");
                requester.close(CloseMode.GRACEFUL);
            }
        });
        requester.start();

        final HttpHost target = new HttpHost("https", "nghttp2.org", 443);
        final String[] requestUris = new String[] {"/httpbin/ip", "/httpbin/user-agent", "/httpbin/headers"};

//        final HttpHost target = new HttpHost("https", "localhost", 8443);
//        final String[] requestUris = new String[] {"/PushServlet"};

        final CountDownLatch latch = new CountDownLatch(requestUris.length);
        for (final String requestUri: requestUris) {
            final Future<AsyncClientEndpoint> future = requester.connect(target, Timeout.ofSeconds(1));
            final AsyncClientEndpoint clientEndpoint = future.get();

            clientEndpoint.execute(
                    new BasicRequestProducer(Methods.GET, target, requestUri),
                    new BasicResponseConsumer<>(new StringAsyncEntityConsumer()),
                    new FutureCallback<Message<HttpResponse, String>>() {

                        @Override
                        public void completed(final Message<HttpResponse, String> message) {
                            System.out.println("Inside complete");
                            clientEndpoint.releaseAndReuse();
                            final HttpResponse response = message.getHead();
                            final String body = message.getBody();
                            System.out.println(requestUri + "->" + response.getCode() + " " + response.getVersion());
                            System.out.println(body);
                            latch.countDown();
                        }

                        @Override
                        public void failed(final Exception ex) {
                            System.out.println("Inside failed");
                            clientEndpoint.releaseAndDiscard();
                            System.out.println(requestUri + "->" + ex);
                            latch.countDown();
                        }

                        @Override
                        public void cancelled() {
                            System.out.println("Inside cancellede");
                            clientEndpoint.releaseAndDiscard();
                            System.out.println(requestUri + " cancelled");
                            latch.countDown();
                        }

                    });
        }

//        latch.await(3, TimeUnit.SECONDS);
        latch.await();
        System.out.println("Shutting down I/O reactor");
        requester.initiateShutdown();
    }
}
