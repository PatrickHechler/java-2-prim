package de.hechler.patrick.j2p;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("javadoc")
public class AlignableInput extends InputStream {
	
	private final InputStream delegate;
	private long              address;
	private long              markAddress = -1L;
	private long              markEnd     = -1L;
	
	public AlignableInput(InputStream in) {
		super();
		this.delegate = in;
	}
	
	/**
	 * align this stream to the given size
	 * 
	 * @param byteSize the size to which should be aligned
	 * 
	 * @throws IOException  if an IO error occurs
	 * @throws EOFException if EOF occurs during the align
	 */
	public void align(int byteSize) throws EOFException, IOException {
		long mod = this.address % byteSize;
		while (mod != 0) {
			mod -= this.delegate.skip(mod);
			if (mod != 0) {
				if (this.delegate.read() == -1) {
					throw new EOFException("reached EOF during align");
				}
				mod--;
			}
		}
	}
	
	public long address() {
		return this.address;
	}
	
	public void address(long address) {
		this.address = address;
	}
	
	@Override
	public void mark(int readlimit) {
		if (this.delegate.markSupported()) {
			this.delegate.mark(readlimit);
			this.markAddress = this.address;
			this.markEnd = this.address + readlimit;
		}
	}
	
	@Override
	public void reset() throws IOException {
		if (!this.delegate.markSupported()) {
			throw new IOException("no mark supported");
		}
		if (this.markAddress == -1) {
			throw new IOException("no mark set");
		}
		if (this.markEnd > this.address) {
			throw new IOException("the mark invalid (too much was read since the mark)");
		}
		this.delegate.reset();
		this.address = this.markAddress;
	}
	
	@Override
	public boolean markSupported() {
		return this.delegate.markSupported();
	}
	
	@Override
	public int read() throws IOException {
		int r = this.delegate.read();
		if (r != -1) this.address++;
		return r;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		int r = this.delegate.read(b);
		this.address += r;
		return r;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int r = this.delegate.read(b, off, len);
		this.address += r;
		return r;
	}
	
	@Override
	public byte[] readAllBytes() throws IOException {
		byte[] r = this.delegate.readAllBytes();
		this.address += r.length;
		return r;
	}
	
	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		int r = this.delegate.readNBytes(b, off, len);
		this.address += r;
		return r;
	}
	
	@Override
	public byte[] readNBytes(int len) throws IOException {
		byte[] r = this.delegate.readNBytes(len);
		this.address += r.length;
		return r;
	}
	
	@Override
	public int available() throws IOException {
		return this.delegate.available();
	}
	
	@Override
	public void close() throws IOException {
		this.delegate.close();
	}
	
	@Override
	public long skip(long n) throws IOException {
		long r = super.skip(n);
		this.address += n;
		return r;
	}
	
	@Override
	public void skipNBytes(long n) throws IOException {
		this.delegate.skipNBytes(n);
		this.address += n;
	}
	
	@Override
	public long transferTo(OutputStream out) throws IOException {
		long r = this.delegate.transferTo(out);
		this.address = r;
		return r;
	}
	
}
