package org.wildfly.swarm.ts.javaee8.servlet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContexts;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import static java.net.http.HttpClient.Version.HTTP_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@RunWith(Arquillian.class)
@DefaultDeployment
public class PushServletTest {
    private static final String EXPECTED_SUCCESS = "<html><head><title>Servlet 4.0</title>" +
            "<link rel=\"stylesheet\" href=\"css/style.css\"/>" +
            "</head><body><p>secure</p><p>pushing succeeded</p></body></html>";

    private static final String EXPECTED_FAILURE = "<html><head><title>Servlet 4.0</title>" +
            "<link rel=\"stylesheet\" href=\"css/style.css\"/>" +
            "</head><body><p>secure</p><p>pushing failed</p></body></html>";

    @Test
    @RunAsClient
    public void serverPushSuccessTest() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, ExecutionException, InterruptedException {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build();
        HttpClient client = HttpClient.newBuilder()
                .version(HTTP_2)
                .sslContext(sslContext)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://localhost:8443/PushServlet"))
                .build();

        AtomicReference<String> pushedResource = new AtomicReference<>();

        CompletableFuture<HttpResponse<String>> httpResponseCompletableFuture =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString(),
                                 (initiatingRequest, pushRequest, acceptor) -> acceptor.apply(HttpResponse.BodyHandlers.ofString())
                                         .thenAccept(resp -> {
                                             pushedResource.set(resp.uri().toString());
                                         }));

        HttpResponse<String> stringHttpResponse = httpResponseCompletableFuture.get();
        assertThat(stringHttpResponse.body()).isEqualTo(EXPECTED_SUCCESS);

        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(pushedResource.get()).isEqualTo("https://localhost:8443/css/style.css");
                });
    }

    @Test
    @RunAsClient
    public void serverPushFailedTest() throws Exception {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build();
        HttpClient client = HttpClient.newBuilder()
                .version(HTTP_2)
                .sslContext(sslContext)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://localhost:8443/PushServlet?forceFail=true"))
                .build();
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        assertThat(body).isEqualTo(EXPECTED_FAILURE);
    }

}