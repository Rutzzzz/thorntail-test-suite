package org.wildfly.swarm.ts.microprofile.rest.client.v13.sslsupport;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "WrongTruststoreCDIClient")
public interface WrongTruststoreCDIClient {
    @GET
    @Path("simple")
    String get();
}
