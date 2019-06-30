package net.tiny.ws.rs.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ChunkedOutputStreamTest {

	public static final String CRLF = "\r\n";

	@Test
	public void testOnceWrite() throws Exception {
		String data = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + CRLF +
			"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" + CRLF +
			"ccccccccc";

		String chunkData = "71" + CRLF +
				"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + CRLF +
				"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" + CRLF +
				"ccccccccc" + CRLF +
				"0" + CRLF;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ChunkedOutputStream cos = new ChunkedOutputStream(baos);
		cos.write(data.getBytes());
		cos.close();

		System.out.println("------------------------");
		System.out.print(new String(baos.toByteArray()));
		System.out.println("------------------------");
		System.out.println();
		assertEquals(chunkData, new String(baos.toByteArray()));
	}

	public void testWrite() throws Exception {
		String chunkData = "46" + CRLF +
				"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + CRLF +
				"1e" + CRLF +
				"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" + CRLF +
				"9" + CRLF +
				"ccccccccc" + CRLF +
				"0" + CRLF;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ChunkedOutputStream cos = new ChunkedOutputStream(baos, 70);
		cos.write("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes());
		cos.flush();
		cos.write("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb".getBytes());
		cos.flush();
		cos.write("ccccccccc".getBytes());
		cos.close();
		System.out.println("------------------------");
		System.out.print(new String(baos.toByteArray()));
		System.out.println("------------------------");
		System.out.println();
		assertEquals(chunkData, new String(baos.toByteArray()));
	}

	public void testChunkWrite() throws Exception {
		String data = "1234567890abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
				"abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890" +
				"{}[]<>-+";

		String chunkData = "24" + CRLF +
				"1234567890abcdefghijklmnopqrstuvwxyz" + CRLF +
				"24" + CRLF +
				"1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ" + CRLF +
				"24" + CRLF +
				"abcdefghijklmnopqrstuvwxyz1234567890" + CRLF +
				"24" + CRLF +
				"ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890" + CRLF +
				"8" + CRLF +
				"{}[]<>-+" + CRLF +
				"0" + CRLF;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ChunkedOutputStream cos = new ChunkedOutputStream(baos, 36);
		cos.write(data.getBytes());
		cos.close();
		System.out.println("------------------------");
		System.out.print(new String(baos.toByteArray()));
		System.out.println("------------------------");
		System.out.println();
		assertEquals(chunkData, new String(baos.toByteArray()));
	}


	public void testChunkIO() throws Exception {
		String data = "1234567890abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
				"abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890" +
				"{}[]<>-+";

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ChunkedOutputStream cos = new ChunkedOutputStream(baos, 36);
		cos.write(data.getBytes());
		cos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ChunkedInputStream cis = new ChunkedInputStream(bais);
		byte[] buffer = new byte[50];
		int size = cis.read(buffer);
		assertEquals(36, size);
		assertEquals("1234567890abcdefghijklmnopqrstuvwxyz",
				new String(buffer, 0, size));

		size = cis.read(buffer);
		assertEquals(36, size);
		assertEquals("1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ",
				new String(buffer, 0, size));

		size = cis.read(buffer);
		assertEquals(36, size);
		assertEquals("abcdefghijklmnopqrstuvwxyz1234567890",
				new String(buffer, 0, size));

		size = cis.read(buffer);
		assertEquals(36, size);
		assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890",
				new String(buffer, 0, size));

		size = cis.read(buffer);
		assertEquals(8, size);
		assertEquals("{}[]<>-+",
				new String(buffer, 0, size));

		size = cis.read(buffer);
		assertEquals(-1, size);

		cis.close();
	}
}
