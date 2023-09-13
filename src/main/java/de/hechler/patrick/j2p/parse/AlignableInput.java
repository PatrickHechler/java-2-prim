// This file is part of the java-2-prim Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.j2p.parse;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("javadoc")
public class AlignableInput extends InputStream {
	
	private final InputStream delegate;
	private long              address;
	private long              markAddress;
	
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
		this.delegate.mark(readlimit);
		this.markAddress = this.address;
	}
	
	@Override
	public void reset() throws IOException {
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
