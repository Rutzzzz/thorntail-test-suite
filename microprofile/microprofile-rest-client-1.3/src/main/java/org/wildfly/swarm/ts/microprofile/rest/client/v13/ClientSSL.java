package org.wildfly.swarm.ts.microprofile.rest.client.v13;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@RegisterRestClient(baseUri = "http://localhost:8080")
public interface ClientSSL {
    @GET
    @Path("/rest/ssl")
    String simpleSSL();
}
