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
	
	enum NullType implements JType {
		TYPE
	}
	
	record ObjectType(String binaryName) implements JType {
		
		@Override
		public String toString() {
			return "L" + this.binaryName + ";";
		}
		
	}
	
	record UninitMe(String binaryName) implements JType {
		
		@Override
		public String toString() {
			return "uninitilized this: L" + this.binaryName + ";";
		}
		
	}
	
	record UninitObjectType(int newAddress, JType type) implements JType {
		
		@Override
		public String toString() {
			return "uninitilized, created at 0x" + Integer.toHexString(this.newAddress) + " : " + this.newAddress + " : " + this.type;
		}
		
	}
	
	record ArrayType(JType componentType) implements JType {
		
		@Override
		public String toString() {
			return "[" + this.componentType.toString();
		}
		
	}
	
}
