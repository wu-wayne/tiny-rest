package net.tiny.ws.rs.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.ParameterFilter;
import net.tiny.ws.SnapFilter;
import net.tiny.ws.VoidHttpHandler;
import net.tiny.ws.WebServiceHandler;
import net.tiny.ws.rs.RestApplication;
import net.tiny.ws.rs.RestServiceFactory;
import net.tiny.ws.rs.RestfulHttpHandler;
import net.tiny.ws.rs.client.RestClient;

public class RestClientTest {

    final int port = 8080;
    EmbeddedServer server;

    @BeforeEach
    public void setUp() throws Exception {
        AccessLogger logger = new AccessLogger();
        ParameterFilter parameter = new ParameterFilter();
        SnapFilter snap = new SnapFilter();

        final Application application = new RestApplication();
        RestServiceFactory factory = new RestServiceFactory();
        factory.setApplication(application);
        WebServiceHandler restful = new RestfulHttpHandler()
                .setFactory(factory)
                .path("/v1/api")
                .filters(Arrays.asList(parameter, logger, snap));

        WebServiceHandler health = new VoidHttpHandler()
                .path("/healthcheck")
                .filter(logger);

        server = new EmbeddedServer.Builder()
                .port(port)
                .handlers(Arrays.asList(restful, health))
                .build();
        server.listen(callback -> {
            if(callback.success()) {
                System.out.println("Server listen on port: " + port);
            } else {
                callback.cause().printStackTrace();
            }
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        server.close();
    }

    @Test
    public void testRestClient() throws Exception {
        RestClient client = new RestClient.Builder()
                .build();

        String response = client.execute("http://localhost:8080/v1/api/test/1234")
                    .get(MediaType.APPLICATION_JSON)
                    .getEntity();
        assertTrue(response.startsWith("{\"Id is 1234\"}"));
    }

}
