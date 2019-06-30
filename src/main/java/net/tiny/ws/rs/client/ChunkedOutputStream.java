package net.tiny.ws.rs.client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ChunkedOutputStream extends BufferedOutputStream {

	public static final int DEFAULT_CHUNK_SIZE = 524288;
	static final byte CR = '\r';
	static final byte LF = '\n';

	public ChunkedOutputStream(final OutputStream out){
		this(out, DEFAULT_CHUNK_SIZE);
	}

	public ChunkedOutputStream(final OutputStream out, final int chunkSize){
		super(out, chunkSize);
	}

	/**
	 * Write a sub-array of bytes.
	 * <P>
	 * The only reason we have to override the BufferedOutputStream version
	 * of this is that it writes the array directly to the output stream
	 * if doesn't fit in the buffer.  So we make it use our own chunk-write
	 * routine instead.  Otherwise this is identical to the parent-class version.
	 * </P>
	 *
	 * @param b the data to be written
	 * @param off the start offset in the data
	 * @param len the number of bytes that are written
	 * @exception IOException if an I/O error occurred
	 *
	 */
	@Override
    public synchronized void write(byte b[], int off, int len ) throws IOException {
    	int avail = buf.length - count;

    	if ( len <= avail ) {
    		System.arraycopy( b, off, buf, count, len );
    		count += len;
    		return; // Over step to do flush()
	    } else if ( len > avail ) {
	    	System.arraycopy( b, off, buf, count, avail);
	    	count += avail;
	    	flush();
	    	System.arraycopy( b, avail, b, 0, (len-avail));
	    	write(b, 0, (len-avail));
	    } else {
	    	writeBuffer(b, off, len);
	    }
	}

	/**
	 *  Flush the stream.  This will write any buffered output bytes as a chunk.
	 *  @exception IOException if an I/O error occurred
	 */
	@Override
    public synchronized void flush() throws IOException {
		if ( count != 0 ) {
		    writeBuffer(buf, 0, count );
		    count = 0;
		}
	}

	@Override
	public void close() throws IOException {
		flush();
		this.out.write('0');
		this.out.write(CR);
		this.out.write(LF);
		this.out.flush();
		super.close();
	}

    /**
     * The only routine that actually writes to the output stream.<BR>
     * This is where chunk semantics are implemented.
     * @param b The data buffer
     * @param offset The offset
     * @param length The length of data to write
     * @throws IOException if an I/O error occurred
     */
    private void writeBuffer( byte b[], int offset, int length) throws IOException
	{
		// Write the chunk length as a hex number.
		final String size = Integer.toHexString(length);
		this.out.write(size.getBytes());
		// Write a CRLF.
		this.out.write( CR );
		this.out.write( LF );
		// Write the data.
		if (length != 0 )
			this.out.write(b, offset, length);
		// Write a CRLF.
		this.out.write( CR );
		this.out.write( LF );
		// And flush the real stream.
		this.out.flush();
	}

}