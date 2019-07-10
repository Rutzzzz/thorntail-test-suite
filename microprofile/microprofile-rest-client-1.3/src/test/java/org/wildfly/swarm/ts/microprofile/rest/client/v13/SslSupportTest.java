package org.wildfly.swarm.ts.microprofile.rest.client.v13;

import org.apache.http.client.fluent.Request;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
@DefaultDeployment
public class SslSupportTest {

//    @Deployment
//    public static Archive<?> deployment() {
//        return ShrinkWrap.create(WebArchive.class)
//                .addPackage(ConfigLocationJwt11Test.class.getPackage())
//                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
//                .addAsManifestResource(new ClassLoaderAsset("config-location.properties"),
//                        "microprofile-config.properties")
//                .addAsResource(new ClassLoaderAsset("project-defaults-security.yml"), "project-defaults.yml")
//                .addAsResource(new ClassLoaderAsset("public-key.pem"), "public-key.pem");
//    }

    @Test
    @RunAsClient
    public void simpleTest() throws IOException {
        String response = Request.Get("http://localhost:8080/rest/client/ssl-support").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from ssl endpoint");
    }
}
