package org.wildfly.swarm.ts.javaee8.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
                 new HttpResponse.PushPromiseHandler<String>() {
                     @Override
                     public void applyPushPromise(HttpRequest initiatingRequest, HttpRequest pushRequest, Function<HttpResponse.BodyHandler<String>, CompletableFuture<HttpResponse<String>>> acceptor) {
                         acceptor.apply(HttpResponse.BodyHandlers.ofString())
                                 .thenAccept(resp -> {
                                     pushedResource.set(resp.uri().toString());
//                                     System.out.println("PR: " + pushedResource.get());
                                 });
                     }
                 });

        HttpResponse<String> stringHttpResponse = httpResponseCompletableFuture.get();
        assertThat(stringHttpResponse.body()).isEqualTo(EXPECTED_SUCCESS);
        Thread.sleep(100); // Bad, very bad
        assertThat(pushedResource.get()).isEqualTo("https://localhost:8443/css/style.css");
    }

    @Test
    @RunAsClient
    public void serverPushFailedTest() throws Exception {
        String response = sendRequest("https://localhost:8443/PushServlet?forceFail=true");
        assertThat(response).isEqualTo(EXPECTED_FAILURE);
    }

    private static String sendRequest(String url) throws IOException, InterruptedException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build();
        HttpClient client = HttpClient.newBuilder()
                .version(HTTP_2)
                .sslContext(sslContext)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

}
