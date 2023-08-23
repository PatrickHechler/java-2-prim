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
