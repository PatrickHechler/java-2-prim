package de.hechler.patrick.j2p.parse;

import java.util.StringJoiner;

@SuppressWarnings("javadoc")
public class Modifier {
	
	private Modifier() {}
	
	public static final int ACC_PUBLIC     = 0x0001;
	public static final int ACC_FINAL      = 0x0010;
	public static final int ACC_SUPER      = 0x0020;
	public static final int ACC_INTERFACE  = 0x0200;
	public static final int ACC_ABSTRACT   = 0x0400;
	public static final int ACC_SYNTHETIC  = 0x1000;
	public static final int ACC_ANNOTATION = 0x2000;
	public static final int ACC_ENUM       = 0x4000;
	public static final int ACC_MODULE     = 0x8000;
	
	public static final int ACC_PRIVATE   = 0x0002;
	public static final int ACC_PROTECTED = 0x0004;
	public static final int ACC_STATIC    = 0x0008;
	public static final int ACC_VOLATILE  = 0x0040;
	public static final int ACC_TRANSIENT = 0x0080;
	
	public static final int ACC_SYNCHRONIZED = 0x0020;
	public static final int ACC_BRIDGE       = 0x0040;
	public static final int ACC_VARARGS      = 0x0080;
	public static final int ACC_NATIVE       = 0x0100;
	public static final int ACC_STRICT       = 0x0800;
	
	public static String methodString(int accessFlags) {
		StringJoiner sj = new StringJoiner(" ");
		if ((accessFlags & ACC_PUBLIC) != 0) sj.add("public");
		if ((accessFlags & ACC_PRIVATE) != 0) sj.add("private");
		if ((accessFlags & ACC_PROTECTED) != 0) sj.add("protected");
		if ((accessFlags & ACC_STATIC) != 0) sj.add("static");
		if ((accessFlags & ACC_FINAL) != 0) sj.add("final");
		if ((accessFlags & ACC_SYNCHRONIZED) != 0) sj.add("synchronized");
		if ((accessFlags & ACC_BRIDGE) != 0) sj.add("bridge");
		if ((accessFlags & ACC_VARARGS) != 0) sj.add("varargs");
		if ((accessFlags & ACC_NATIVE) != 0) sj.add("native");
		if ((accessFlags & ACC_ABSTRACT) != 0) sj.add("abstract");
		if ((accessFlags & ACC_STRICT) != 0) sj.add("strict");
		if ((accessFlags & ACC_SYNTHETIC) != 0) sj.add("synthetic");
		return sj.toString();
	}
	
	public static String fieldString(int accessFlags) {
		StringJoiner sj = new StringJoiner(" ");
		if ((accessFlags & ACC_PUBLIC) != 0) sj.add("public");
		if ((accessFlags & ACC_PRIVATE) != 0) sj.add("private");
		if ((accessFlags & ACC_PROTECTED) != 0) sj.add("protected");
		if ((accessFlags & ACC_STATIC) != 0) sj.add("static");
		if ((accessFlags & ACC_FINAL) != 0) sj.add("final");
		if ((accessFlags & ACC_TRANSIENT) != 0) sj.add("transient");
		if ((accessFlags & ACC_VOLATILE) != 0) sj.add("volatile");
		if ((accessFlags & ACC_SYNTHETIC) != 0) sj.add("synthetic");
		if ((accessFlags & ACC_ENUM) != 0) sj.add("enum");
		return sj.toString();
	}
	
	public static String classString(int accessFlags) {
		StringJoiner sj = new StringJoiner(" ");
		if ((accessFlags & ACC_PUBLIC) != 0) sj.add("public");
		if ((accessFlags & ACC_FINAL) != 0) sj.add("final");
		if ((accessFlags & ACC_SUPER) != 0) sj.add("super");
		if ((accessFlags & ACC_INTERFACE) != 0) sj.add("interface");
		if ((accessFlags & ACC_ABSTRACT) != 0) sj.add("abstract");
		if ((accessFlags & ACC_SYNTHETIC) != 0) sj.add("synthetic");
		if ((accessFlags & ACC_ANNOTATION) != 0) sj.add("annotation");
		if ((accessFlags & ACC_ENUM) != 0) sj.add("enum");
		if ((accessFlags & ACC_MODULE) != 0) sj.add("module");
		return sj.toString();
	}
	
}
