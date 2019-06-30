package net.tiny.ws.rs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.ws.BaseWebService;
import net.tiny.ws.HttpHandlerHelper;
import net.tiny.ws.RequestHelper;
import net.tiny.ws.ResponseHeaderHelper;

public class RestfulHttpHandler extends BaseWebService {

    private RestServiceFactory factory;

    public RestServiceFactory getFactory() {
        return this.factory;
    }

    public RestfulHttpHandler setFactory(RestServiceFactory factory) {
        this.factory = factory;
        return this;
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);

        final Map<String, Object> args = new HashMap<>();
        RestServiceHandler handler = factory.getRestServiceHandler(request.getURI(),
                request.getMethod(), args);
        if (null == handler) {
            // Not found service
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            LOGGER.fine(String.format("[REST] - '%s' 404 Not found", request.getURI()));
            return;
        }

        if (!handler.acceptableMediaType(MIME_TYPE.JSON.name())) {
            //TODO
            //he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_ACCEPTABLE, -1);
            LOGGER.fine(String.format("[REST] - '%s' 404 Not found", request.getURI()));
            //return;
        }

        Object[] params = factory.convertArguments(he, args, handler.getMethod());
        Object result = handler.invoke(params);
        final String response = JsonParser.marshal(result);
        final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        header.setContentType(MIME_TYPE.JSON);
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponse.length);
        he.getResponseBody().write(rawResponse);
    }

}
