package net.tiny.ws.rs.client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ChunkedInputStream extends FilterInputStream {

	static final byte CR = '\r';
	static final byte LF = '\n';
	private static final int MIN_LAST_CHUNK_LENGTH = "\r\n0\r\n\r\n".length();
	private static final int NO_CHUNK_YET = -1;

	private boolean closed;
	private boolean hasMoreChunks = true;
	private int bytesRemainingInChunk = NO_CHUNK_YET;

	public ChunkedInputStream(final InputStream in){
		super(in);
	}

	@Override
	public int available() throws IOException {
		if (closed) {
			throw new IOException("stream closed");
		}
		return hasMoreChunks ? Math.min(in.available(), bytesRemainingInChunk) : 0;
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
	}


	@Override
	public int read(byte[] buffer, int offset, int count) throws IOException {
		if (closed) {
			throw new IOException("stream closed");
		}
		if (!hasMoreChunks) {
			return -1;
		}
		if (bytesRemainingInChunk == 0 || bytesRemainingInChunk == NO_CHUNK_YET) {
			readChunkSize();
			if (!hasMoreChunks) {
				return -1;
			}
		}
		int read = in.read(buffer, offset,
				Math.min(count, bytesRemainingInChunk));
		if (read == -1) {
			throw new IOException("unexpected end of stream");
		}
		bytesRemainingInChunk -= read;
		if (bytesRemainingInChunk == 0
				&& in.available() >= MIN_LAST_CHUNK_LENGTH) {
			readChunkSize();
		}
		return read;
	}

	private void readChunkSize() throws IOException {
		// read the suffix of the previous chunk
		if (bytesRemainingInChunk != NO_CHUNK_YET) {
			readLine(in);
		}
		String chunkSizeString = readLine(in);
		int index = chunkSizeString.indexOf(";");
		if (index != -1) {
			chunkSizeString = chunkSizeString.substring(0, index);
		}
		try {
			bytesRemainingInChunk = Integer
					.parseInt(chunkSizeString.trim(), 16);
		} catch (NumberFormatException e) {
			throw new IOException("Expected a hex chunk size, but was "
					+ chunkSizeString);
		}
		if (bytesRemainingInChunk == 0) {
			hasMoreChunks = false;
		}
	}

	String readLine(InputStream in) throws IOException {
		StringBuilder result = new StringBuilder(80);
		while (true) {
			int c = in.read();
			if (c == -1) {
				return result.toString();
			} else if (c == LF) {
				break;
			}
			result.append((char) c);
		}
		int length = result.length();
		if (length > 0 && result.charAt(length - 1) == CR) {
			result.setLength(length - 1);
		}
		return result.toString();
	}

}
