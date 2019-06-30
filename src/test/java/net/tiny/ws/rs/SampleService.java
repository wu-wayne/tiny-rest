package net.tiny.ws.rs;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

@Path("calc")
public class SampleService {

    @GET
    @Path("add/{a}/{b}")
    @Produces(MediaType.TEXT_HTML)
    public String add(@PathParam("a")double a, @PathParam("b")double b) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>JSR311 Example</title></head><body>");
        sb.append(String.format(" %1$.3f + %2$.3f = %3$.3f", a, b, (a+b)));
        sb.append("</body></html>");
        return sb.toString();
    }

    @POST
    @Path("divide")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String divide(@FormParam("divisor")Double divisor,
            @FormParam("roundingMode")Double roundingMode) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>JSR311 Example</title></head><body>");
        sb.append(String.format(" %1$.3f + %2$.3f = %3$.3f", divisor, roundingMode, (divisor/roundingMode)));
        sb.append("</body></html>");
        return sb.toString();
    }

    @GET
    @Path(value = "cookie")
    public Response cookie(@CookieParam("myCookie1") Cookie cookie1, @CookieParam("myCookie2") Cookie cookie2) {
        String response = "cookie1: " + cookie1.getValue() +  "  cookie2: " + cookie2.getValue();
        NewCookie c1 = new NewCookie("name1", "The cookie value$1");
        Cookie cookie = new Cookie("name2", "The cookie value%2");
        NewCookie c2 = new NewCookie(cookie);
        ResponseBuilder builder = Response.ok(response);
        return builder.cookie(c1, c2).build();
    }

    @GET
    @Path("query")
    public Response query(
        @DefaultValue("100") @QueryParam("from") int from,
        @DefaultValue("999") @QueryParam("to") int to,
        @DefaultValue("name") @QueryParam("orderBy") List<String> orderBy) {

        String response = "getUsers is called, from : " + from + ", to : " + to
        + ", orderBy" + orderBy.toString();

        return Response
           .status(200)
           .entity(response)
           .build();
    }


    @GET
    @Path("info")
    public Response info(@Context UriInfo info) {
        String from = info.getQueryParameters().getFirst("from");
        String to = info.getQueryParameters().getFirst("to");
        List<String> orderBy = info.getQueryParameters().get("orderBy");
        return Response.status(200)
           .entity("getUsers is called, from : " + from + ", to : " + to
            + ", orderBy" + orderBy.toString()).build();

    }

    @POST
    @Path("post")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void post(@Context UriInfo uri) throws IOException {
        MultivaluedMap<String, String> formParam  = uri.getQueryParameters();
        formParam.getFirst("");
        //TODO
        //response.sendRedirect("");
    }

}

