package org.wildfly.swarm.ts.javaee8.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import static java.net.http.HttpClient.Version.HTTP_2;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
@DefaultDeployment
public class MappingServletTest {
    @RunAsClient
    @Test
    public void contextRootMatch() throws IOException, InterruptedException {
        String response = sendRequestWithUrlPattern("");
        String expected = getExpectedResponse("CONTEXT_ROOT", servletName,
                                              "", "");
        assertThat(response).isEqualTo(expected);
    }

    @RunAsClient
    @Test
    public void defaultMatch() throws IOException, InterruptedException {
        String response = sendRequestWithUrlPattern("/nonexistent");
        String expected = getExpectedResponse("DEFAULT", servletName,
                                              "", "/");
        assertThat(response).isEqualTo(expected);
    }

    @RunAsClient
    @Test
    public void exactMatch() throws IOException, InterruptedException {
        String response = sendRequestWithUrlPattern("/MappingServlet");
        String expected = getExpectedResponse("EXACT", servletName,
                                              "MappingServlet", "/MappingServlet");
        assertThat(response).isEqualTo(expected);
    }

    @RunAsClient
    @Test
    public void extensionMatch() throws IOException, InterruptedException {
        String response = sendRequestWithUrlPattern("/Some/Text.txt");
        String expected = getExpectedResponse("EXTENSION", servletName,
                                              "Some/Text", "*.txt");
        assertThat(response).isEqualTo(expected);
    }

    @RunAsClient
    @Test
    public void pathMatch() throws IOException, InterruptedException {
        String response = sendRequestWithUrlPattern("/Path/file");
        String expected = getExpectedResponse("PATH", servletName,
                                              "file", "/Path/*");
        assertThat(response).isEqualTo(expected);
    }

    private static String sendRequestWithUrlPattern(String urlPattern) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://localhost:8080" + urlPattern))
                .build();
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String getExpectedResponse(String mappingMatch, String servletName, String matchingValue, String pattern) {
        return "Mapping match: " + mappingMatch + "\nServlet name: " + servletName + "\nMatch value: " +
                matchingValue + "\nPattern: " + pattern + "\n";
    }

    private final String servletName = MappingServlet.class.getName();

}
