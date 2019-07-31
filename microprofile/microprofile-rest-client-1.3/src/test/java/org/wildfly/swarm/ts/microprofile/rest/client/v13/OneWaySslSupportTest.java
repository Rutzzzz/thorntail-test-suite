package org.wildfly.swarm.ts.microprofile.rest.client.v13;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;

import org.apache.http.ssl.SSLContexts;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.ts.microprofile.rest.client.v13.hostnameverifiers.AcceptAllVerifier;
import org.wildfly.swarm.ts.microprofile.rest.client.v13.hostnameverifiers.DenyAllVerifier;
import org.wildfly.swarm.ts.microprofile.rest.client.v13.sslsupport.OneWayCDIClient;
import org.wildfly.swarm.ts.microprofile.rest.client.v13.sslsupport.WrongTruststoreCDIClient;

@RunWith(Arquillian.class)
public class OneWaySslSupportTest extends AbstractSslSupportTest {

    @Inject
    @RestClient
    private OneWayCDIClient clientOneWay;

    @Inject
    @RestClient
    private WrongTruststoreCDIClient clientWrongTruststore;

    @Deployment
    public static Archive<?> deployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class)
                .addPackages(true, OneWaySslSupportTest.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")

                .addAsManifestResource("client-cert.cer")
                .addAsManifestResource("client-keystore.jks")
                .addAsManifestResource("client-truststore.jks")
                .addAsManifestResource("server-cert.cer")
                .addAsManifestResource("server-keystore.jks")
                .addAsManifestResource("server-truststore.jks")
                .addAsManifestResource("unknown-client-keystore.jks")

                .addAsManifestResource(new ClassLoaderAsset("META-INF/microprofile-config.properties"), "microprofile-config.properties")
                .addAsResource(new ClassLoaderAsset("project-defaults-with-one-way-authentication.yml"), "project-defaults.yml");
        return archive;
    }

    @Test
    public void testCDIWithTruststore() {
        String str = clientOneWay.get();
        Assert.assertEquals("Hello from endpoint", str);
    }

    @Test(expected = ProcessingException.class)
    public void testCDIWithIncorrectTruststore() {
        clientWrongTruststore.get();
    }

    @Test
    @RunAsClient
    public void testProgrammaticWithTruststore() {
        String str = getClient(correctTruststore).get();
        Assert.assertEquals("Hello from endpoint", str);
    }

    @Test
    @RunAsClient
    public void testProgrammaticWithTrustStoreUsingSSLContext() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, CertificateException, IOException {
        SSLContext context = SSLContexts.custom().loadTrustMaterial(clientTruststore, clientPassword).build();
        String str = getClient(context).get();
        Assert.assertEquals("Hello from endpoint", str);
    }

    @Test(expected = ProcessingException.class)
    @RunAsClient
    public void testProgrammaticWithIncorrectTruststore() {
        getClient(incorrectTruststore).get();
    }

    @Test
    @RunAsClient
    public void testProgrammaticVerifierAcceptAll() {
        getClient(correctTruststore, null, new AcceptAllVerifier()).get();
    }

    @Test(expected = ProcessingException.class)
    @RunAsClient
    public void testProgrammaticVerifierDenyAll() {
        getClient(correctTruststore, null, new DenyAllVerifier()).get();
    }

}
