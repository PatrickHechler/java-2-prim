package de.hechler.patrick.j2p;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClassReader {
	
	public ClassFile read(DataInput in) throws IOException {
		checkMagic(in.readInt());
		int minor = in.readUnsignedShort();
		int major = in.readUnsignedShort();
		checkVersion(minor, major);
		int       cpCountP1 = in.readUnsignedShort();
		CPEntry[] entries   = new CPEntry[cpCountP1 - 1];
		for (int i = 1; i < cpCountP1; i++) {
			entries[i - 1] = readCPEntry(in, minor, major, entries, i);
		}
		throw new UnsupportedOperationException();
	}
	
	private static CPEntry readCPEntry(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		int tag = in.readUnsignedByte();
		return switch (tag) {
		case 1 -> readCPUtf8(in, minor, major, entries, i);
		case 3 -> readCPInteger(in, minor, major, entries, i);
		case 4 -> readCPFloat(in, minor, major, entries, i);
		case 5 -> readCPLong(in, minor, major, entries, i);
		case 6 -> readCPDouble(in, minor, major, entries, i);
		case 7 -> readCPClass(in, minor, major, entries, i);
		case 8 -> readCPString(in, minor, major, entries, i);
		case 9 -> readCPFieldref(in, minor, major, entries, i);
		case 10 -> readCPMethodref(in, minor, major, entries, i);
		case 11 -> readCPInterfaceMethodref(in, minor, major, entries, i);
		case 12 -> readCPNameAndType(in, minor, major, entries, i);
		case 15 -> readCPMethodHandle(in, minor, major, entries, i);
		case 16 -> readCPMethodType(in, minor, major, entries, i);
		case 17 -> readCPDynamic(in, minor, major, entries, i);
		case 18 -> readCPInvokeDynamic(in, minor, major, entries, i);
		case 19 -> readCPModule(in, minor, major, entries, i);
		case 20 -> readCPPackage(in, minor, major, entries, i);
		default -> throw new VerifyError("invalid constant pool entry tag: " + tag);
		};
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPUtf8(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPEUtf8(in.readUTF());
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPInteger(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPEInt(in.readInt());
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPFloat(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPEFloat(in.readFloat());
	}
	
	private static CPEntry readCPLong(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPELong(in.readLong());
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPDouble(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPEDouble(in.readDouble());
	}
	
	private static CPEntry readCPClass(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int nameIndex = in.readUnsignedShort();
		if (nameIndex == 0 || nameIndex > entries.length) {
			throw new VerifyError("the name index is outside of the allowed range: 1.." + entries.length + " index: " + nameIndex + " current index is: " + i + ")");
		}
		CPEntry.CPEClass cls = new CPEntry.CPEClass(nameIndex);
		if (nameIndex < i) {
			if (entries[i] instanceof CPEntry.CPEUtf8 u8) {
				cls.initType(readType(u8.val(), false));
			} else {
				throw new VerifyError("this entries name index refers to a non utf8 entry (current index is: " + i + ")");
			}
		}
		return cls;
	}
	
	private static CPEntry readCPString(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int nameIndex = in.readUnsignedShort();
		if (nameIndex == 0 || nameIndex > entries.length) {
			throw new VerifyError("the name index is outside of the allowed range: 1.." + entries.length + " index: " + nameIndex + " current index is: " + i + ")");
		}
		CPEntry.CPEString str = new CPEntry.CPEString(nameIndex);
		if (nameIndex < i) {
			if (entries[i] instanceof CPEntry.CPEUtf8 u8) {
				str.initStr(u8.val());
			} else {
				throw new VerifyError("this entries name index refers to a non utf8 entry (current index is: " + i + ")");
			}
		}
		return str;
	}
	
	private static CPEntry readCPFieldref(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int classIndex = in.readUnsignedShort();
		int nameAndTypeIndex = in.readUnsignedShort();
		if (classIndex == 0 || classIndex > entries.length) {
			throw new VerifyError("the class index is outside of the allowed range: 1.." + entries.length + " index: " + classIndex + " current index is: " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError("the name_and_type index is outside of the allowed range: 1.." + entries.length + " index: " + nameAndTypeIndex + " current index is: " + i + ")");
		}
		return new CPEntry.CPEFieldRef(classIndex, nameAndTypeIndex);
	}
	
	private static CPEntry readCPMethodref(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int classIndex = in.readUnsignedShort();
		int nameAndTypeIndex = in.readUnsignedShort();
		if (classIndex == 0 || classIndex > entries.length) {
			throw new VerifyError("the class index is outside of the allowed range: 1.." + entries.length + " index: " + classIndex + " current index is: " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError("the name_and_type index is outside of the allowed range: 1.." + entries.length + " index: " + nameAndTypeIndex + " current index is: " + i + ")");
		}
		return new CPEntry.CPEMethodRef(classIndex, nameAndTypeIndex);
	}
	
	private static CPEntry readCPInterfaceMethodref(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int classIndex = in.readUnsignedShort();
		int nameAndTypeIndex = in.readUnsignedShort();
		if (classIndex == 0 || classIndex > entries.length) {
			throw new VerifyError("the class index is outside of the allowed range: 1.." + entries.length + " index: " + classIndex + " current index is: " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError("the name_and_type index is outside of the allowed range: 1.." + entries.length + " index: " + nameAndTypeIndex + " current index is: " + i + ")");
		}
		return new CPEntry.CPEInterfaceMethodRef(classIndex, nameAndTypeIndex);
	}
	
	private static CPEntry readCPNameAndType(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int nameIndex = in.readUnsignedShort();
		int typeIndex = in.readUnsignedShort();
		if (nameIndex == 0 || nameIndex > entries.length) {
			throw new VerifyError("the name index is outside of the allowed range: 1.." + entries.length + " index: " + nameIndex + " current index is: " + i + ")");
		}
		if (typeIndex == 0 || typeIndex > entries.length) {
			throw new VerifyError("the type index is outside of the allowed range: 1.." + entries.length + " index: " + typeIndex + " current index is: " + i + ")");
		}
		return new CPEntry.CPENameAndType(nameIndex, typeIndex);
	}
	
	private static CPEntry readCPMethodHandle(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV51_0(minor, major);
		int refKind = in.readUnsignedByte();
		int refIndex = in.readUnsignedShort();
		if (refKind == 0 || refKind > 9) {
			throw new VerifyError("refernece kind is invalid: " + refKind);
		}
		if (refIndex == 0 || refIndex > entries.length) {
			throw new VerifyError("the reference index is outside of the allowed range: 1.." + entries.length + " index: " + refIndex + " current index is: " + i + ")");
		}
		return new CPEntry.CPEMethodHandle(refKind, refIndex);
	}
	
	private static CPEntry readCPMethodType(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV51_0(minor, major);
		int mtypeIndex = in.readUnsignedShort();
		return new CPEntry.CPEMethodType(mtypeIndex);
	}
	
	private static CPEntry readCPDynamic(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV55_0(minor, major);
		int bootstrapMetAttrIndex = in.readUnsignedShort();
		int nameAndTypeIndex = in.readUnsignedShort();
		if (bootstrapMetAttrIndex == 0 || bootstrapMetAttrIndex > entries.length) {
			throw new VerifyError("the bootstrap_method_attr_name index is outside of the allowed range: 1.." + entries.length + " index: " + bootstrapMetAttrIndex + " current index is: " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError("the name_and_type index is outside of the allowed range: 1.." + entries.length + " index: " + nameAndTypeIndex + " current index is: " + i + ")");
		}
		return new CPEntry.CPEDynamic(bootstrapMetAttrIndex, nameAndTypeIndex);
	}
	
	private static CPEntry readCPInvokeDynamic(DataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV51_0(minor, major);
		int bootstrapMetAttrIndex = in.readUnsignedShort();
		int nameAndTypeIndex = in.readUnsignedShort();
		if (bootstrapMetAttrIndex == 0 || bootstrapMetAttrIndex > entries.length) {
			throw new VerifyError("the bootstrap_method_attr_name index is outside of the allowed range: 1.." + entries.length + " index: " + bootstrapMetAttrIndex + " current index is: " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError("the name_and_type index is outside of the allowed range: 1.." + entries.length + " index: " + nameAndTypeIndex + " current index is: " + i + ")");
		}
		return new CPEntry.CPEInvokeDynamic(bootstrapMetAttrIndex, nameAndTypeIndex);
	}
	
	private static CPEntry readCPModule(DataInput in, int minor, int major, CPEntry[] entries, int i) {
		checkV53_0(minor, major);
		
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static CPEntry readCPPackage(DataInput in, int minor, int major, CPEntry[] entries, int i) {
		checkV53_0(minor, major);
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private MethodType readMethodType(DataInput in) throws IOException {
		read(in, '(');
		List<JType> params = new ArrayList<>();
		while (true) {
			int c = in.readUnsignedByte();
			if (c == ')') break;
			params.add(readType0(c, in, false));
		}
		JType ret = readType(in, true);
		return new MethodType(params, ret);
	}
	
	private static JType readType(String str, boolean allowVoid) throws IOException {
		ByteArrayInputStream baos = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
		DataInputStream      in   = new DataInputStream(baos);
		JType                res  = readType(in, allowVoid);
		if (baos.available() != 0) {
			throw new VerifyError("did not use the complete string to read the type! string: '" + str + "'");
		}
		return res;
	}
	
	private static JType readType(DataInput in, boolean allowVoid) throws IOException {
		int c = in.readUnsignedByte();
		return readType0(c, in, allowVoid);
	}
	
	private static JType readType0(int c, DataInput in, boolean allowVoid) throws IOException {
		return switch (c) {
		case 'B' -> JType.JPrimType.BYTE;
		case 'C' -> JType.JPrimType.CHAR;
		case 'D' -> JType.JPrimType.DOUBLE;
		case 'F' -> JType.JPrimType.FLOAT;
		case 'I' -> JType.JPrimType.INT;
		case 'J' -> JType.JPrimType.LONG;
		case 'S' -> JType.JPrimType.SHORT;
		case 'Z' -> JType.JPrimType.BOOLEAN;
		case 'L' -> {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while (true) {
				c = in.readUnsignedByte();
				if (c == ';') break;
				baos.write(c);
			}
			byte[] arr        = baos.toByteArray();
			String binaryName = new String(arr, 0, arr.length, StandardCharsets.UTF_8);
			yield new JType.ObjectType(binaryName);
		}
		case '[' -> {
			JType.ArrayType res = new JType.ArrayType(readType(in, false));
			int             d   = 1;
			for (JType.ArrayType at = res; at.componentType() instanceof JType.ArrayType it; at = it) {
				if (++d > 255) {
					throw new ClassFormatError("invalid type (represents more than 255 array dimensions)");
				}
			}
			yield res;
		}
		case 'V' -> {
			if (allowVoid) yield JType.JPrimType.VOID;
			throw new ClassFormatError("invalid type: " + (char) c);
		}
		default -> throw new ClassFormatError("invalid type: " + (char) c);
		};
	}
	
	private static void read(DataInput in, int b) throws IOException {
		int r = in.readUnsignedByte();
		if (r != b) {
			throw new ClassFormatError("expected to read " + (char) b + " but got " + (char) r);
		}
	}
	
	private static void checkVn_n(int minMinor, int minMajor, String se, int minor, int major) {
		if (major < minMajor || (major == minMajor && minor < minMinor)) {
			throw new VerifyError("the class file uses a feature of a later version (needed " + minMajor + "." + minMinor + " (JavaSE " + se
					+ ")) but class file version is " + major + "." + minor);
		}
	}
	
	private static void checkV45_3(int minor, int major) {
		checkVn_n(3, 45, "1.0.2", minor, major);
	}
	
	private static void checkV49_0(int minor, int major) {
		checkVn_n(0, 49, "5", minor, major);
	}
	
	private static void checkV51_0(int minor, int major) {
		checkVn_n(0, 51, "7", minor, major);
	}
	
	private static void checkV53_0(int minor, int major) {
		checkVn_n(0, 53, "9", minor, major);
	}
	
	private static void checkV55_0(int minor, int major) {
		checkVn_n(0, 55, "11", minor, major);
	}
	
	private static void checkVersion(int minor, int major) {
		// JavaSE 20 is 64, newer Java is not supported
		// oldest is 45 (JavaSE 1.0.2)
		// if major is 56 or above minor must be 0 (or 65535 for preview features)
		// before major 56 minor can have any value
		// preview features are only valid for 64 (JavaSE 20)
		if (major > 64) {
			throw new UnsupportedClassVersionError("I only support versions up to 64 (JavaSE 20)! major: " + major);
		}
		if (major >= 56) { // min JavaSE 12, preview features exist now
			if (minor != 0) {
				if (minor != 65535) {
					throw new ClassFormatError("invalid minor version number: " + minor + " major: " + major);
				}
				if (major != 64) { // only support JavaSE 20 preview features
					throw new UnsupportedClassVersionError("preview features enabled in an invalid release: " + major + " (only support JavaSE 20 : 64)");
				}
			}
		} else if (major < 45) {
			throw new UnsupportedClassVersionError("I only support versions higher or equal to 45 (JavaSE 1.0.2)! major: " + major);
		}
	}
	
	private static void checkMagic(int magic) {
		if (magic != 0xCAFEBABE) {
			throw new ClassFormatError("wrong magic: 0x" + Integer.toHexString(magic));
		}
	}
	
}
