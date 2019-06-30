package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import net.tiny.config.JsonParser;
import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.ParameterFilter;
import net.tiny.ws.SnapFilter;
import net.tiny.ws.WebServiceHandler;
import net.tiny.ws.client.SimpleClient;


public class JsonServerTest {

    @Test
    public void testJsonServer() throws Exception {
        final int port = 8081;
        AccessLogger logger = new AccessLogger();
        ParameterFilter parameter = new ParameterFilter();
        SnapFilter snap = new SnapFilter();

        WebServiceHandler controller = new JsonHttpHandler()
                .path("/v1/api")
                .filters(Arrays.asList(parameter, snap, logger));

        EmbeddedServer server = new EmbeddedServer.Builder()
                .port(port)
                .handlers(Arrays.asList(controller))
                .build();
        server.listen(callback -> {
            if(callback.success()) {
                System.out.println("Server listen on port: " + port);
            } else {
                callback.cause().printStackTrace();
            }
        });

        SimpleClient client = new SimpleClient.Builder()
                .redirects(true)
                .build();

        client.doGet(new URL("http://localhost:" + port +"/v1/api/js?q=svg&t=min"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("application/json;charset=utf-8", client.getHeader("Content-Type"));
                final String response = new String(client.getContents());
                System.out.println(response);
                Map map = JsonParser.unmarshal(response, Map.class);
                assertEquals(2, map.size());
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        client.close();


        server.close();
    }

}
