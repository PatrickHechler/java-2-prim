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


public record JField(int accessFlags, String name, JType descriptor, CPEntry initialValue) {
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("JField [accessFlags=").append(this.accessFlags).append(" : 0x").append(Integer.toHexString(this.accessFlags)).append(" : ")
				.append(Modifier.fieldString(this.accessFlags)).append(", name=").append(this.name).append(", descriptor=").append(this.descriptor);
		if (this.initialValue != null) {
			b.append(", initialValue=").append(this.initialValue);
		}
		return b.append(']').toString();
	}
	
}
