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
	
	ObjectInfo CLASS_TYPE = new JSMEVerificationInfo.ObjectInfo(new JType.ObjectType("java/lang/Class"));
	ObjectInfo OBJECT_TYPE = new JSMEVerificationInfo.ObjectInfo(new JType.ObjectType("java/lang/Object"));
	
}
