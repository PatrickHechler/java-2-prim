package de.hechler.patrick.j2p;


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
	
}
