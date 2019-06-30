package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

public class RestServiceFactoryTest {

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty("javax.ws.rs.scan.packages.include", "net.tiny.*");
        System.setProperty("javax.ws.rs.scan.packages.exclude", "java.*, com.sun.*");
        System.setProperty("javax.ws.rs.logging.level", "info");
    }

    @AfterEach
    public void tearDown() throws Exception {
        System.getProperties().remove("javax.ws.rs.scan.packages.include");
        System.getProperties().remove("javax.ws.rs.scan.packages.exclude");
        System.getProperties().remove("javax.ws.rs.logging.level");
    }

    @Test
    public void testFecthRestService() throws Exception {
        final Application application = new RestApplication();
        RestServiceFactory factory = new RestServiceFactory();
        factory.setApplication(application);
        final Map<String, Object> args = new HashMap<>();
        RestServiceHandler handler = factory.getRestServiceHandler("/v1/api/test/123", "GET", args);
        assertNotNull(handler);
        assertTrue(handler instanceof MethodPattern);

        Object target = handler.getTarget();
        assertNotNull(target);
        assertTrue(target instanceof TestService);

        assertEquals(1, args.size());
        assertEquals("123", args.get("id"));

        Object ret = handler.invoke(new Object[] {"123"});
        assertNotNull(ret);
        assertEquals("Id is 123", ret);
    }

    @Test
    public void testNotFoundRestService() throws Exception {
        final Application application = new RestApplication();
        RestServiceFactory factory = new RestServiceFactory();
        factory.setApplication(application);
        final Map<String, Object> args = new HashMap<>();
        RestServiceHandler handler = factory.getRestServiceHandler("/v1/api/unkonw/123", "GET", args);
        assertNull(handler);
    }
}
