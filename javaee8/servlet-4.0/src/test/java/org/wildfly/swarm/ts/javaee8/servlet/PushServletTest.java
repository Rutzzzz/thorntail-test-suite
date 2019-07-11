package org.wildfly.swarm.ts.javaee8.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

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
    public void serverPushSuccessTest() throws Exception {
        String response = sendRequest("https://localhost:8443/PushServlet");
        assertThat(response).isEqualTo(EXPECTED_SUCCESS);
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
