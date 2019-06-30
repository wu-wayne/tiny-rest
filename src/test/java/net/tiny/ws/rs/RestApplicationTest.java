package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import javax.ws.rs.core.Application;

import org.junit.jupiter.api.Test;

public class RestApplicationTest {

    @Test
    public void testFindRestClass() throws Exception {
        System.setProperty("javax.ws.rs.scan.packages.include", "net.tiny.*");
        System.setProperty("javax.ws.rs.scan.packages.exclude", "java.*, com.sun.*");
        System.setProperty("javax.ws.rs.logging.level", "info");

        final Application application = new RestApplication();
        Set<Class<?>> serviceClasses = application.getClasses();
        System.out.println("Application.getClasses() " + serviceClasses.size()); //4
        System.out.println(serviceClasses);
        assertFalse(serviceClasses.isEmpty());

        System.getProperties().remove("javax.ws.rs.scan.packages.include");
        System.getProperties().remove("javax.ws.rs.scan.packages.exclude");
        System.getProperties().remove("javax.ws.rs.logging.level");
    }
}
