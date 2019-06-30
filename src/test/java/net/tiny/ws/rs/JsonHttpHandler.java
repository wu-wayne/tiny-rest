package net.tiny.ws.rs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.ws.BaseWebService;
import net.tiny.ws.HttpHandlerHelper;
import net.tiny.ws.ResponseHeaderHelper;

public class JsonHttpHandler extends BaseWebService {


    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        switch (method) {
        case GET:
            doGet(he);
            break;
        case POST:
            doPost(he);
            break;
        case PUT:
            doPut(he);
            break;
        case DELETE:
            doDelete(he);
            break;
        default:
            break;
        }
    }

    protected void doGet(HttpExchange he) throws IOException {
        final Map<String, List<String>> request = HttpHandlerHelper.getRequestHelper(he).getParameters();
        System.out.println("### " + request);
        // do something with the request parameters
        final String response = JsonParser.marshal(request);
        final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        header.setContentType(MIME_TYPE.JSON);
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponse.length);
        he.getResponseBody().write(rawResponse);
    }

    protected void doPost(HttpExchange he) throws IOException {

    }

    protected void doPut(HttpExchange he) throws IOException {

    }

    protected void doDelete(HttpExchange he) throws IOException {

    }

}
