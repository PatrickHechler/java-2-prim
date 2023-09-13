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
public sealed interface JSMEVerificationInfo {
	
	public enum SimpleInfo implements JSMEVerificationInfo {
		
		TOP, //
		INTEGER, //
		FLOAT, //
		LONG, //
		DOUBLE, //
		NULL, //
		UNINITIALIZEDTHIS,//
	
	}
	
	public record ObjectInfo(JType type) implements JSMEVerificationInfo {}
	
	public record UninitilizedInfo(int newAddress) implements JSMEVerificationInfo {}
	
	ObjectInfo METHOD_TYPE_TYPE = new JSMEVerificationInfo.ObjectInfo(new JType.ObjectType("java/lang/invoke/MethodType"));
	ObjectInfo METHOD_HANDLE_TYPE = new JSMEVerificationInfo.ObjectInfo(new JType.ObjectType("java/lang/invoke/MethodHandle"));
	ObjectInfo STRING_TYPE = new JSMEVerificationInfo.ObjectInfo(new JType.ObjectType("java/lang/String"));
	ObjectInfo CLASS_TYPE  = new JSMEVerificationInfo.ObjectInfo(new JType.ObjectType("java/lang/Class"));
	ObjectInfo OBJECT_TYPE = new JSMEVerificationInfo.ObjectInfo(new JType.ObjectType("java/lang/Object"));
	
}
