package net.tiny.ws.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/api/test")
public class TestService {
    private String id;

    @GET
    @Path("{id}")
    @Produces(value = MediaType.APPLICATION_JSON)
    public String getId(@PathParam("id")String id) {
        setId(id);
        return "Id is " + id;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
