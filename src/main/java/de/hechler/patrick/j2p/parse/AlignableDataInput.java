package de.hechler.patrick.j2p.parse;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("javadoc")
public class AlignableDataInput extends DataInputStream {
	
	public AlignableDataInput(InputStream in) {
		super(in instanceof AlignableInput ? in : new AlignableInput(in));
	}
	
	/**
	 * align this stream to the given size
	 * 
	 * @param byteSize the size to which should be aligned
	 * 
	 * @throws IOException if an IO error occurs
	 * 
	 * @see AlignableInput#align(int)
	 */
	public void align(int byteSize) throws IOException {
		((AlignableInput) super.in).align(byteSize);
	}
	
	public long address() {
		return ((AlignableInput) super.in).address();
	}
	
	public void address(long address) {
		((AlignableInput) super.in).address(address);
	}
	
}
