package net.tiny.ws.rs;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path(value = "/context")
@Consumes(value = MediaType.APPLICATION_JSON)
public class TestContext {

    @GET
    @Produces(value = MediaType.APPLICATION_JSON)
    public String getValue(
            @QueryParam(value = "key") String key,
            @QueryParam(value = "value") String value) throws Exception {
        return String.format("The key of '%s' is '%s", value, key);
    }
}
