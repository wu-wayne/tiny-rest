package net.tiny.ws.rs.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class RestClientInnerTest {

    @Test
    public void testRequestPath() throws Exception {
        RestClient client = new RestClient.Builder()
        		.userAgent(RestClient.BROWSER_AGENT)
                .build();
        RestClient.Request request;
        request = client.execute("http://localhost:8080")
        			.path("/v1/api/test/12345");
        assertEquals("http://localhost:8080/v1/api/test/12345", request.url.toString());

        request = client.execute("http://localhost:8080/")
    			.path("/v1/api/test/12345");
        assertEquals("http://localhost:8080/v1/api/test/12345", request.url.toString());

        request = client.execute("http://localhost:8080/v0/gw")
    			.path("v1/api/test/12345");
        assertEquals("http://localhost:8080/v1/api/test/12345", request.url.toString());
    }

    @Test
    public void testQueryParam() throws Exception {
        RestClient client = new RestClient.Builder()
        		.userAgent(RestClient.BROWSER_AGENT)
                .build();
        RestClient.Request request;
        request = client.execute("http://localhost:8080/v1/api/s?q=a&t=1")
        			.queryParam("n", 1, 2, 3);
        assertEquals("http://localhost:8080/v1/api/s?q=a&t=1&n=1&n=2&n=3", request.url.toString());

        request = client.execute("http://localhost:8080/v1/api/s?q=a&t=1")
    			.queryParam("t", 2, 3);
        assertEquals("http://localhost:8080/v1/api/s?q=a&t=1&t=2&t=3", request.url.toString());

        request = client.execute("http://localhost:8080/v1/api/s")
    			.queryParam("t", "a", "b");
        assertEquals("http://localhost:8080/v1/api/s?t=a&t=b", request.url.toString());

        request = client.execute("http://localhost:8080/v1/api/s")
    			.queryParam("t", "日本語", "中文");
        assertEquals("http://localhost:8080/v1/api/s?t=%E6%97%A5%E6%9C%AC%E8%AA%9E&t=%E4%B8%AD%E6%96%87", request.url.toString());
    }
}
