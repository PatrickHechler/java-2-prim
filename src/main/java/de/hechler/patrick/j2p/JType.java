package de.hechler.patrick.j2p;


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
