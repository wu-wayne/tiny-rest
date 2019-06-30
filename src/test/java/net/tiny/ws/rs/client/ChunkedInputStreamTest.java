package net.tiny.ws.rs.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;


public class ChunkedInputStreamTest {

	static final String LS = System.getProperty("line.separator");

	@Test
	public void testRead() throws Exception {
		String data = "46" + LS +
			"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + LS +
			"1e" + LS +
			"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" + LS +
			"9" + LS +
			"ccccccccc" + LS +
			"0" + LS;

		ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes());
		ChunkedInputStream cis = new ChunkedInputStream(bais);
		byte[] buffer = new byte[50];
		int size = cis.read(buffer);
		assertEquals(50, size);
		assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
				new String(buffer, 0, size));

		size = cis.read(buffer);
		assertEquals(20, size);
		assertEquals("aaaaaaaaaaaaaaaaaaaa",
				new String(buffer, 0, size));

		buffer = new byte[1024];
		size = cis.read(buffer);
		assertEquals(30, size);
		assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
				new String(buffer, 0, size));

		size = cis.read(buffer);
		assertEquals(9, size);
		assertEquals("ccccccccc",
				new String(buffer, 0, size));

		size = cis.read(buffer);
		assertEquals(-1, size);

		cis.close();
	}

}
