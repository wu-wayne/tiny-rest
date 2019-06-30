package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PathPatternTest {

    @Test
    public void testCheckPattern() {
        try {
            PathPattern.checkPattern("///");
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            PathPattern.checkPattern("/{id}//");
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            PathPattern.checkPattern("/a{id}//");
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            PathPattern.checkPattern("/{id}/}}/");
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            PathPattern.checkPattern("/{id}/{{/");
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            PathPattern.checkPattern("/{id}/{}/");
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        assertEquals(1, PathPattern.checkPattern("/u/{id}/"));
        assertEquals(1, PathPattern.checkPattern("/{id}/u/"));
        assertEquals(2, PathPattern.checkPattern("/{id}/u/{weiboid}"));
    }

    @Test
    public void testCheckQueryPattern() {
        assertEquals(3, PathPattern.checkPattern("/customer/{zip : \\d+}/{city : [a-z]*}/{dumy}"));
        assertEquals(3, PathPattern.checkPattern("/customer;{zip=\\d+};{city=[a-z]*};{dumy}"));
        assertEquals(3, PathPattern.checkPattern("/customer?{zip=\\d+}&{city=[a-z]*}&{dumy}"));
    }

    @Test
    public void testComparePattern() throws IllegalArgumentException {
        assertEquals(1, PathPattern.comparePattern("/u/{id}/a", "/z/{id}/"));
        assertEquals(1, PathPattern.comparePattern("/z/{id}/a", "/z/{id}/"));
        assertEquals(1, PathPattern.comparePattern("/u/{id}/a", "/u/"));
        assertEquals(1, PathPattern.comparePattern("/u/{id}/a", "/u/{id}/"));
        assertEquals(1, PathPattern.comparePattern("/u/{id}/a", "/l/{id}/"));
        //		try {
        assertEquals(1, PathPattern.comparePattern("/u/{id}/a", "/u/a/"));
        //			fail();
        //		} catch (final Exception ex) {
        //			assertTrue(ex instanceof IllegalArgumentException);
        //		}
        //		try {
        assertEquals(-1, PathPattern.comparePattern(
                "/rest/{mapDbName}/map/{reqType}/{method}",
                "/rest/{mapDbName}/map/layer/field/{method}"));

        assertEquals(0, PathPattern.comparePattern("/u/{id}/a", "/u/{id}/a"));
        assertEquals(-1, PathPattern.comparePattern("/u/", "/u/{id}/a"));
        assertEquals(1, PathPattern.comparePattern("/u/", null));
        assertEquals(-1, PathPattern.comparePattern(null, "/u/"));
        assertEquals(0, PathPattern.comparePattern(null, null));
        assertEquals(1, PathPattern.comparePattern("/u/", ""));
        assertEquals(-1, PathPattern.comparePattern("", "/u/"));
        assertEquals(0, PathPattern.comparePattern("", ""));
        assertEquals(0, PathPattern.comparePattern("", null));
    }

    @Test
    public void testComparePatternAndUrl() throws IllegalArgumentException {
        assertEquals(1, PathPattern.comparePatternAndUrl("/u/{id}/a", "/z/1111/"));
        assertEquals(1, PathPattern.comparePatternAndUrl("/z/{id}/a", "/z/1111/"));
        assertEquals(1, PathPattern.comparePatternAndUrl("/u/{id}/a", "/u/"));
        assertEquals(1, PathPattern.comparePatternAndUrl("/u/{id}/a", "/u/1111/"));
        assertEquals(1, PathPattern.comparePatternAndUrl("/u/{id}/a", "/l/1111/"));
        assertEquals(-1, PathPattern.comparePatternAndUrl("/u/{id}/a", "/x/1111/a"));
        assertEquals(0, PathPattern.comparePatternAndUrl("/u/{id}/a", "/u/1111/a"));
        assertEquals(-1, PathPattern.comparePatternAndUrl("/u/", "/u/1111/a"));
        assertEquals(1, PathPattern.comparePatternAndUrl("/u/", null));
        assertEquals(-1, PathPattern.comparePatternAndUrl(null, "/u/"));
        assertEquals(0, PathPattern.comparePatternAndUrl(null, null));
        assertEquals(1, PathPattern.comparePatternAndUrl("/u/", ""));
        assertEquals(-1, PathPattern.comparePatternAndUrl("", "/u/"));
        assertEquals(0, PathPattern.comparePatternAndUrl("", ""));
        assertEquals(0, PathPattern.comparePatternAndUrl("", null));
        assertEquals(-1, PathPattern.comparePatternAndUrl(
                "/rest/{mapDbName}/map/{reqType}/{method}",
                "/rest/testdb/map/layer/field/add"));


    }

    @Test
    public void testValidatePatterns() {

        try {
            PathPattern.validatePattern("/u/112/{var}", "/u/{id}/var", '{', '}');
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            PathPattern.validatePattern("/u/112/", "/u/{id}/", '{', '}');
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            PathPattern.validatePattern("/{mapDbName}/layer/{method}",
                    "/manage/map/create", '{', '}');
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            PathPattern.validatePattern("/u/112/{var}", "/u/112/var", '{', '}');
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {//
            PathPattern.validatePattern("/u/112/{var}", "/u/{id}/var", '{', '}');
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {//
            PathPattern.validatePattern("/u/112/{var}", "/u/{id}/", '{', '}');
            fail();
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }

    }
}
