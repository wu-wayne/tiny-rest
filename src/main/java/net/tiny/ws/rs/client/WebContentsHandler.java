package net.tiny.ws.rs.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public abstract class WebContentsHandler {

    private static final int MAX_BUFFER_SIZE   = 4 * 1024 * 1024;

	protected abstract String getHeader(String name);
	private boolean cached = false;
	private int contentLength = 0;
	private int chunkSize = ChunkedOutputStream.DEFAULT_CHUNK_SIZE;
	private HttpURLConnection connection;

	public void setConnection(HttpURLConnection conn) {
    	connection = conn;
    }

	public void close() {
		if(null == connection) {
			throw new IllegalStateException("Not yet connected");
		}
		connection.disconnect();
	}

	public int getStatusCode() throws IOException {
		if(null == connection) {
			throw new IllegalStateException("Not yet connected");
		}
    	return connection.getResponseCode();
	}

	public Map<String, List<String>> getResponseHeaders() {
		if(null == connection) {
			throw new IllegalStateException("Not yet connected");
		}
		return connection.getHeaderFields();
	}

	public int cacheContents(OutputStream out) throws IOException {
		if (null == connection) {
			throw new IllegalStateException("Not yet connected.");
		}
		if (cached) {
			throw new IllegalStateException("The contents had been read.");
		}
		int size = setContents(connection.getInputStream(), out);
		cached = true;
		return size;
	}

	protected int setContents(InputStream in, OutputStream out) throws IOException {
		// Cache contents data
		if (isChunked()) {
			return setChunkedContents(new ChunkedInputStream(in), out);
		} else if (isCompressed()) {
			return setComprssedContents(new GZIPInputStream(in), out);
		} else {
			contentLength = getContentLength();
			int bufferSize = MAX_BUFFER_SIZE;
			if(contentLength > 0) {
				bufferSize = contentLength;
			}
			int total = 0;
			byte buffer[] = new byte[bufferSize];
			int len = 0;
			while((len = in.read(buffer)) > 0 ) {
				out.write(buffer, 0, len);
				total += len;
			}
			out.flush();
			return total;
		}
	}

	private int setChunkedContents(ChunkedInputStream in, OutputStream out) throws IOException {
		int total = 0;
		byte buffer[] = new byte[chunkSize];
		int len = 0;
		while((len = in.read(buffer)) > 0 ) {
			out.write(buffer, 0, len);
			total += len;
			out.flush();
		}
		return total;
	}

	private int setComprssedContents(GZIPInputStream in, OutputStream out) throws IOException {
		int total = 0;
		// create a buffer of maximum size
		byte buffer[] = new byte[MAX_BUFFER_SIZE];
		int len = 0;
		while((len = in.read(buffer)) > 0 ) {
			out.write(buffer, 0, len);
			total += len;
			out.flush();
		}
		return total;
	}


	protected boolean isChunked() {
		String value = getHeader("Transfer-Encoding");
		if (value == null)
			return false;
		return value.equalsIgnoreCase("Chunked");
	}

	protected boolean isCompressed() {
		String value = getHeader("Content-Encoding");
		if (value == null)
			return false;
		return value.equalsIgnoreCase("gzip");
	}

	protected int getContentLength() {
        String value = getHeader("Content-length");
        return value != null ? Integer.parseInt(value) : -1;
	}
}
