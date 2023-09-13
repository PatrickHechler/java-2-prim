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


@SuppressWarnings("javadoc")
public sealed interface JType {
	
	enum JPrimType implements JType {
		
		BYTE, CHAR, DOUBLE, FLOAT, INT, SHORT, LONG, BOOLEAN, VOID;
		
		@Override
		public String toString() {
			return switch (this) {
			case BYTE -> "B";
			case CHAR -> "C";
			case DOUBLE -> "D";
			case FLOAT -> "F";
			case INT -> "I";
			case LONG -> "L";
			case SHORT -> "S";
			case BOOLEAN -> "Z";
			case VOID -> "V";
			};
		}
		
	}
	
	enum NullType implements JType { TYPE }
	
	record ObjectType(String binaryName) implements JType {
		
		@Override
		public String toString() {
			return "L" + this.binaryName + ";";
		}
		
	}
	
	record ArrayType(JType componentType) implements JType {
		
		@Override
		public String toString() {
			return "[" + this.componentType.toString();
		}
		
	}
	
}
