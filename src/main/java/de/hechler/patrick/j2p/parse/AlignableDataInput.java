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
