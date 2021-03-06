package org.wildfly.swarm.ts.javaee.jaxrs.jaxb;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class HelloResource {
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Hello hello() {
        return new Hello("WORLD");
    }
}
