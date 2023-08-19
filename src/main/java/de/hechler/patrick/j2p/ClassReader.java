package de.hechler.patrick.j2p;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.hechler.patrick.j2p.JCommand.SimpleCommands;

@SuppressWarnings("javadoc")
public class ClassReader {
	
	public ClassFile read(AlignableDataInput in) throws IOException {
		checkMagic(in.readInt());
		ClassFile f = readConstantPool(in);
		readAccessAndMyNames(in, f);
		readInterfaces(in, f);
		readFields(in, f);
		readMethods(in, f);
		readAttributes(in, f);
		return f;
	}
	
	private static void readAttributes(AlignableDataInput in, ClassFile f) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static void readMethods(AlignableDataInput in, ClassFile f) throws IOException {
		final int len  = in.readUnsignedShort();
		JMethod[] mets = new JMethod[len];
		for (int i = 0; i < len; i++) {
			int        accessFlags = in.readUnsignedShort();
			String     name        = f.cp(CPEntry.CPEUtf8.class, in.readUnsignedShort()).val();
			MethodType methodType  = readMethodType(f.cp(CPEntry.CPEUtf8.class, in.readUnsignedShort()).val());
			JMethod    method      = new JMethod(f, accessFlags, name, methodType);
			final int  alen        = in.readUnsignedShort();
			for (int ai = 0; ai < alen; ai++) {
				String attrName = f.cp(CPEntry.CPEUtf8.class, in.readUnsignedShort()).val();
				if ("Code".equals(attrName)) {
					in.readInt();
					int        maxStack  = in.readUnsignedShort();
					int        maxLocals = in.readUnsignedShort();
					final long codeEnd   = in.readInt() & 0xFFFFFFFFL;
					in.address(0L);
					method.initCode(maxStack, maxLocals);
					while (in.address() < codeEnd) {
						long     addr = in.address();
						JCommand cmd  = readCommand(in, method);
						cmd.initAdress(addr);
						method.addCommand(cmd);
					}
					if (in.address() != codeEnd) {
						throw new ClassFormatError("the code segment did not end at the promised address!");
					}
				} else {
					skipAttribute(in);
				}
			}
			mets[i] = method;
		}
		f.init3(mets);
	}
	
	public enum JVMType {
		
		VOID,
		
		REFERENCE,
		
		BYTE_BOOL, SHORT,
		
		INT, LONG, FLOAT, DOUBLE, CHAR
	
	}
	
	public enum JVMMath {
		
		ADD, SUB, MUL, DIV, MOD,
		
		NEG,
		
		AND, OR, XOR, SHIFT_LEFT, SHIFT_ARITMETIC_RIGTH, SHIFT_LOGIC_RIGTH,
		
		NOT
	
	}
	
	public enum JVMCmp {
		
		EQUAL, NOT_EQUAL,
		
		LOWER, LOWER_EQUAL,
		
		GREATER, GREATER_EQUAL
	
	}
	
	private static JCommand readCommand(AlignableDataInput in, JMethod method) throws IOException {
		int b = in.readUnsignedByte();
		return switch (b) {
		case 0x32 -> readArrayLoad(in, method, JVMType.REFERENCE);
		case 0x53 -> readArrayStore(in, method, JVMType.REFERENCE);
		case 0x01 -> readConst(in, method, JVMType.REFERENCE, 0L);
		case 0x19 -> readLocalLoad(in, method, JVMType.REFERENCE, -1, false); // modifiable by wide
		case 0x2A, 0x2B, 0x2C, 0x2D -> readLocalLoad(in, method, JVMType.REFERENCE, b - 0x2A, false);
		case 0xBD -> readNewArray(in, method, true); // reference array
		case 0xB0 -> readReturn(in, method, JVMType.REFERENCE);
		case 0xBE -> readArrayLength(in, method);
		case 0x3A -> readLocalStore(in, method, JVMType.REFERENCE, -1, false); // modifiable by wide
		case 0x4B, 0x4C, 0x4D, 0x4E -> readLocalStore(in, method, JVMType.REFERENCE, b - 0x4B, false);
		case 0xBF -> readAThrow(in, method);
		case 0x33 -> readArrayLoad(in, method, JVMType.BYTE_BOOL);
		case 0x54 -> readArrayStore(in, method, JVMType.BYTE_BOOL);
		case 0x10 -> readPush(in, method, JVMType.BYTE_BOOL);
		case 0x34 -> readArrayLoad(in, method, JVMType.CHAR);
		case 0x55 -> readArrayStore(in, method, JVMType.CHAR);
		case 0xC0 -> readCheckCast(in, method, true);
		case 0x90 -> readPrimConvert(in, method, JVMType.DOUBLE, JVMType.FLOAT);
		case 0x8E -> readPrimConvert(in, method, JVMType.DOUBLE, JVMType.INT);
		case 0x8F -> readPrimConvert(in, method, JVMType.DOUBLE, JVMType.LONG);
		case 0x63 -> readPrimMath(in, method, JVMType.DOUBLE, JVMMath.ADD);
		case 0x31 -> readArrayLoad(in, method, JVMType.DOUBLE);
		case 0x52 -> readArrayStore(in, method, JVMType.DOUBLE);
		case 0x98 -> readFPCompare(in, method, JVMType.DOUBLE, 1);
		case 0x97 -> readFPCompare(in, method, JVMType.DOUBLE, -1);
		case 0x0E -> readConst(in, method, JVMType.DOUBLE, Double.doubleToRawLongBits(0D));
		case 0x0F -> readConst(in, method, JVMType.DOUBLE, Double.doubleToRawLongBits(1D));
		case 0x6F -> readPrimMath(in, method, JVMType.DOUBLE, JVMMath.DIV);
		case 0x18 -> readLocalLoad(in, method, JVMType.DOUBLE, -1, false); // modifiable by wide
		case 0x26, 0x27, 0x28, 0x29 -> readLocalLoad(in, method, JVMType.DOUBLE, b - 0x26, false);
		case 0x6B -> readPrimMath(in, method, JVMType.DOUBLE, JVMMath.MUL);
		case 0x77 -> readPrimMath(in, method, JVMType.DOUBLE, JVMMath.NEG);
		case 0x73 -> readPrimMath(in, method, JVMType.DOUBLE, JVMMath.MOD);
		case 0xAF -> readReturn(in, method, JVMType.DOUBLE);
		case 0x39 -> readLocalStore(in, method, JVMType.DOUBLE, -1, false); // modifiable by wide
		case 0x47, 0x48, 0x49, 0x4A -> readLocalStore(in, method, JVMType.DOUBLE, b - 0x47, false);
		case 0x67 -> readPrimMath(in, method, JVMType.DOUBLE, JVMMath.SUB);
		case 0x59, 0x5A, 0x5B -> readStackDup(in, method, b - 0x59, 1); // skipped entries // duplicate 1 entry
		case 0x5C, 0x5D, 0x5E -> readStackDup(in, method, b - 0x5C, 2); // skipped entries // duplicate 2 entries
		case 0x8D -> readPrimConvert(in, method, JVMType.FLOAT, JVMType.DOUBLE);
		case 0x8B -> readPrimConvert(in, method, JVMType.FLOAT, JVMType.INT);
		case 0x8C -> readPrimConvert(in, method, JVMType.FLOAT, JVMType.LONG);
		case 0x62 -> readPrimMath(in, method, JVMType.FLOAT, JVMMath.ADD);
		case 0x30 -> readArrayLoad(in, method, JVMType.FLOAT);
		case 0x51 -> readArrayStore(in, method, JVMType.FLOAT);
		case 0x96 -> readFPCompare(in, method, JVMType.FLOAT, 1);
		case 0x95 -> readFPCompare(in, method, JVMType.FLOAT, -1);
		case 0x0B -> readConst(in, method, JVMType.DOUBLE, Float.floatToRawIntBits(0F));
		case 0x0C -> readConst(in, method, JVMType.DOUBLE, Float.floatToRawIntBits(1F));
		case 0x0D -> readConst(in, method, JVMType.DOUBLE, Float.floatToRawIntBits(2F));
		case 0x6E -> readPrimMath(in, method, JVMType.FLOAT, JVMMath.DIV);
		case 0x17 -> readLocalLoad(in, method, JVMType.FLOAT, -1, false); // modifiable by wide
		case 0x22, 0x23, 0x24, 0x25 -> readLocalLoad(in, method, JVMType.FLOAT, b - 0x22, false);
		case 0x6A -> readPrimMath(in, method, JVMType.FLOAT, JVMMath.MUL);
		case 0x76 -> readPrimMath(in, method, JVMType.FLOAT, JVMMath.NEG);
		case 0x72 -> readPrimMath(in, method, JVMType.FLOAT, JVMMath.MOD);
		case 0xAE -> readReturn(in, method, JVMType.FLOAT);
		case 0x38 -> readLocalStore(in, method, JVMType.FLOAT, -1, false); // modifiable by wide
		case 0x43, 0x44, 0x45, 0x46 -> readLocalStore(in, method, JVMType.FLOAT, b - 0x43, false);
		case 0x66 -> readPrimMath(in, method, JVMType.FLOAT, JVMMath.SUB);
		case 0xB4 -> readGetField(in, method);
		case 0xB2 -> readGetStaticField(in, method);
		case 0xA7 -> readGoto(in, method, false); // non-wide
		case 0xC8 -> readGoto(in, method, true); // wide
		case 0x91 -> readPrimConvert(in, method, JVMType.INT, JVMType.BYTE_BOOL);
		case 0x92 -> readPrimConvert(in, method, JVMType.INT, JVMType.CHAR);
		case 0x87 -> readPrimConvert(in, method, JVMType.INT, JVMType.DOUBLE);
		case 0x86 -> readPrimConvert(in, method, JVMType.INT, JVMType.FLOAT);
		case 0x85 -> readPrimConvert(in, method, JVMType.INT, JVMType.LONG);
		case 0x93 -> readPrimConvert(in, method, JVMType.INT, JVMType.SHORT);
		case 0x60 -> readPrimMath(in, method, JVMType.INT, JVMMath.ADD);
		case 0x2E -> readArrayLoad(in, method, JVMType.INT);
		case 0x7E -> readPrimMath(in, method, JVMType.INT, JVMMath.AND);
		case 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 -> readConst(in, method, JVMType.INT, (0x03 - b)); // 0x02 is -1
		case 0x6C -> readPrimMath(in, method, JVMType.INT, JVMMath.DIV);
		case 0xA5 -> readCompare(in, method, JVMType.REFERENCE, JVMCmp.EQUAL);
		case 0xA6 -> readCompare(in, method, JVMType.REFERENCE, JVMCmp.NOT_EQUAL);
		case 0x9F -> readCompare(in, method, JVMType.INT, JVMCmp.EQUAL);
		case 0xA0 -> readCompare(in, method, JVMType.INT, JVMCmp.NOT_EQUAL);
		case 0xA1 -> readCompare(in, method, JVMType.INT, JVMCmp.LOWER);
		case 0xA2 -> readCompare(in, method, JVMType.INT, JVMCmp.GREATER_EQUAL);
		case 0xA3 -> readCompare(in, method, JVMType.INT, JVMCmp.GREATER);
		case 0xA4 -> readCompare(in, method, JVMType.INT, JVMCmp.LOWER_EQUAL);
		case 0x99 -> readSign(in, method, JVMType.INT, JVMCmp.EQUAL);
		case 0x9A -> readSign(in, method, JVMType.INT, JVMCmp.NOT_EQUAL);
		case 0x9B -> readSign(in, method, JVMType.INT, JVMCmp.LOWER);
		case 0x9C -> readSign(in, method, JVMType.INT, JVMCmp.GREATER_EQUAL);
		case 0x9D -> readSign(in, method, JVMType.INT, JVMCmp.GREATER);
		case 0x9E -> readSign(in, method, JVMType.INT, JVMCmp.LOWER_EQUAL);
		case 0xC7 -> readSign(in, method, JVMType.REFERENCE, JVMCmp.NOT_EQUAL);
		case 0xC6 -> readSign(in, method, JVMType.REFERENCE, JVMCmp.EQUAL);
		case 0x84 -> readIInc(in, method, false); // modifiable by wide
		case 0x15 -> readLocalLoad(in, method, JVMType.INT, -1, false); // modifiable by wide
		case 0x1A, 0x1B, 0x1C, 0x1D -> readLocalLoad(in, method, JVMType.FLOAT, b - 0x1A, false);
		case 0x68 -> readPrimMath(in, method, JVMType.INT, JVMMath.MUL);
		case 0x74 -> readPrimMath(in, method, JVMType.INT, JVMMath.NEG);
		case 0xC1 -> readCheckCast(in, method, false);
		case 0xBA -> readInvokeDynamic(in, method);
		case 0xB9 -> readInvokeInterface(in, method);
		case 0xB7 -> readInvokeSpecial(in, method);
		case 0xB8 -> readInvokeStatic(in, method);
		case 0xB6 -> readInvokeVirtual(in, method);
		case 0x80 -> readPrimMath(in, method, JVMType.INT, JVMMath.OR);
		case 0x70 -> readPrimMath(in, method, JVMType.INT, JVMMath.MOD);
		case 0xAC -> readReturn(in, method, JVMType.INT);
		case 0x78 -> readPrimMath(in, method, JVMType.INT, JVMMath.SHIFT_LEFT);
		case 0x7A -> readPrimMath(in, method, JVMType.INT, JVMMath.SHIFT_ARITMETIC_RIGTH);
		case 0x36 -> readLocalStore(in, method, JVMType.INT, -1, false); // modifiable by wide
		case 0x3B, 0x3C, 0x3D, 0x3E -> readLocalStore(in, method, JVMType.INT, b - 0x3B, false);
		case 0x64 -> readPrimMath(in, method, JVMType.INT, JVMMath.SUB);
		case 0x7C -> readPrimMath(in, method, JVMType.INT, JVMMath.SHIFT_LOGIC_RIGTH);
		case 0x82 -> readPrimMath(in, method, JVMType.INT, JVMMath.XOR);
		case 0xA8, 0xC9 -> readJSR(in, method, b); // maybe just fail (not supported since JavaSE 7)
		case 0x8A -> readPrimConvert(in, method, JVMType.LONG, JVMType.DOUBLE);
		case 0x89 -> readPrimConvert(in, method, JVMType.LONG, JVMType.FLOAT);
		case 0x88 -> readPrimConvert(in, method, JVMType.LONG, JVMType.INT);
		case 0x61 -> readPrimMath(in, method, JVMType.LONG, JVMMath.ADD);
		case 0x2F -> readArrayLoad(in, method, JVMType.LONG);
		case 0x7F -> readPrimMath(in, method, JVMType.LONG, JVMMath.AND);
		case 0x50 -> readArrayStore(in, method, JVMType.LONG);
		case 0x94 -> readLongCompare(in, method);
		case 0x09 -> readConst(in, method, JVMType.LONG, 0L);
		case 0x0A -> readConst(in, method, JVMType.LONG, 1L);
		case 0x12 -> readLoadConstPool(in, method, false, 1);
		case 0x13 -> readLoadConstPool(in, method, true, 1);
		case 0x14 -> readLoadConstPool(in, method, true, 2);
		case 0x6D -> readPrimMath(in, method, JVMType.LONG, JVMMath.DIV);
		case 0x16 -> readLocalLoad(in, method, JVMType.LONG, -1, false); // modifiable by wide
		case 0x1E, 0x1F, 0x20, 0x21 -> readLocalLoad(in, method, JVMType.LONG, b - 0x1E, false);
		case 0x69 -> readPrimMath(in, method, JVMType.LONG, JVMMath.MUL);
		case 0x75 -> readPrimMath(in, method, JVMType.LONG, JVMMath.NEG);
		case 0xAB -> readLookupSwitch(in, method);
		case 0x81 -> readPrimMath(in, method, JVMType.LONG, JVMMath.OR);
		case 0x71 -> readPrimMath(in, method, JVMType.LONG, JVMMath.MOD);
		case 0xAD -> readReturn(in, method, JVMType.LONG);
		case 0x79 -> readPrimMath(in, method, JVMType.LONG, JVMMath.SHIFT_LEFT);
		case 0x7B -> readPrimMath(in, method, JVMType.LONG, JVMMath.SHIFT_ARITMETIC_RIGTH);
		case 0x37 -> readLocalStore(in, method, JVMType.LONG, -1, false); // modifiable by wide
		case 0x3F, 0x40, 0x41, 0x42 -> readLocalStore(in, method, JVMType.REFERENCE, b - 0x3F, false);
		case 0x65 -> readPrimMath(in, method, JVMType.LONG, JVMMath.SUB);
		case 0x7D -> readPrimMath(in, method, JVMType.LONG, JVMMath.SHIFT_LOGIC_RIGTH);
		case 0x83 -> readPrimMath(in, method, JVMType.LONG, JVMMath.XOR);
		case 0xC2 -> readMonitorEnter(in, method);
		case 0xC3 -> readMonitorExit(in, method);
		case 0xC5 -> readMultiNewArray(in, method);
		case 0xBB -> readNew(in, method);
		case 0xBC -> readNewArray(in, method, false); // primitive array
		case 0x00 -> readNOP(in, method);
		case 0x57, 0x58 -> readPOP(in, method, b - 0x56); // 1 or 2 pop operations
		case 0xB5 -> readSetField(in, method);
		case 0xB3 -> readSetStaticField(in, method);
		case 0xA9 -> readRet(in, method, false); // non-wide // maybe just fail (not usable since JavaSE 7, since JSR is not supported)
		case 0xB1 -> readReturn(in, method, JVMType.VOID);
		case 0x35 -> readArrayLoad(in, method, JVMType.SHORT);
		case 0x56 -> readArrayStore(in, method, JVMType.SHORT);
		case 0x11 -> readPush(in, method, JVMType.SHORT);
		case 0x5F -> readSwap(in, method);
		case 0xAA -> readTableSwitch(in, method);
		case 0xC4 -> readWide(in, method);
		default -> throw new ClassFormatError("unknown command: " + b + " : 0x" + Integer.toHexString(b));
		};
	}
	
	private static JCommand readNewArray(AlignableDataInput in, JMethod method, boolean reference) throws IOException {
		JType type;
		if (reference) {
			type = method.file.cp(CPEntry.CPEClass.class, in.readUnsignedShort()).type();
		} else {
			int t = in.readUnsignedByte();
			type = switch (t) {
			case 4 -> JType.JPrimType.BOOLEAN;
			case 5 -> JType.JPrimType.CHAR;
			case 6 -> JType.JPrimType.FLOAT;
			case 7 -> JType.JPrimType.DOUBLE;
			case 8 -> JType.JPrimType.BYTE;
			case 9 -> JType.JPrimType.SHORT;
			case 10 -> JType.JPrimType.INT;
			case 11 -> JType.JPrimType.LONG;
			default -> throw new ClassFormatError("invalid primitive type number: " + t);
			};
		}
		return new JCommand.NewArray(type);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readArrayLength(AlignableDataInput in, JMethod method) {
		return new JCommand.SimpleCommand(SimpleCommands.ARRAY_LENGTH);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readAThrow(AlignableDataInput in, JMethod method) {
		return new JCommand.SimpleCommand(SimpleCommands.A_THROW);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readFPCompare(AlignableDataInput in, JMethod method, JVMType type, int nanValue) {
		return new JCommand.FPCompare(type, nanValue);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readStackDup(AlignableDataInput in, JMethod method, int skip, int dupCnt) {
		return new JCommand.StackDup(skip, dupCnt);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readGetField(AlignableDataInput in, JMethod method) {
		return new JCommand.SimpleCommand(SimpleCommands.GET_FIELD);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readGetStaticField(AlignableDataInput in, JMethod method) {
		return new JCommand.SimpleCommand(SimpleCommands.GET_STATIC_FIELD);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readGoto(AlignableDataInput in, JMethod method, boolean wide) throws IOException {
		return new JCommand.Goto(in.readUnsignedShort());
	}
	
	@SuppressWarnings("unused")
	private static JCommand readCompare(AlignableDataInput in, JMethod method, JVMType type, JVMCmp cmp) throws IOException {
		return new JCommand.Compare(in.readUnsignedShort(), type, cmp);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readSign(AlignableDataInput in, JMethod method, JVMType type, JVMCmp cmp) throws IOException {
		return new JCommand.Sign(in.readUnsignedShort(), type, cmp);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readIInc(AlignableDataInput in, JMethod method, boolean wide) throws IOException {
		int index = wide ? in.readUnsignedShort() : in.readUnsignedByte();
		int addConst = wide ? in.readShort() : in.readByte();
		return new JCommand.IInc(index, addConst);
	}
	
	private static JCommand readCheckCast(AlignableDataInput in, JMethod method, boolean fail) throws IOException { // fail = false -> instanceof (null is handled differently!)
		JType type = method.file.cp(CPEntry.CPEClass.class, in.readUnsignedShort()).type();
		return new JCommand.CheckCast(type, fail);
	}
	
	private static JCommand readInvokeDynamic(AlignableDataInput in, JMethod method) throws IOException {
		CPEntry.CPEInvokeDynamic id = method.file.cp(CPEntry.CPEInvokeDynamic.class, in.readUnsignedShort());
		readShort(in, 0);
		return new JCommand.InvokeDynamic(id);
	}
	
	private static JCommand readInvokeInterface(AlignableDataInput in, JMethod method) throws IOException {
		CPEntry.CPEInterfaceMethodRef imr = method.file.cp(CPEntry.CPEInterfaceMethodRef.class, in.readUnsignedShort());
		in.readUnsignedByte();
		readByte(in, 0);
		return new JCommand.InvokeInterface(imr);
	}
	
	private static JCommand readInvokeSpecial(AlignableDataInput in, JMethod method) throws IOException {
		CPEntry.CPENormalMethodRef mr = method.file.cp(CPEntry.CPENormalMethodRef.class, in.readUnsignedShort());
		return new JCommand.InvokeSpecial(mr);
	}
	
	private static JCommand readInvokeStatic(AlignableDataInput in, JMethod method) throws IOException {
		CPEntry.CPENormalMethodRef mr = method.file.cp(CPEntry.CPENormalMethodRef.class, in.readUnsignedShort());
		return new JCommand.InvokeStatic(mr);
	}
	
	private static JCommand readInvokeVirtual(AlignableDataInput in, JMethod method) throws IOException {
		CPEntry.CPEMethodRef mr = method.file.cp(CPEntry.CPEMethodRef.class, in.readUnsignedShort());
		return new JCommand.InvokeVirtual(mr);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readJSR(AlignableDataInput in, JMethod method, int b) {
		throw new InternalError("I do not support ret, jsr, jsr_w, wide ret");
	}
	
	@SuppressWarnings("unused")
	private static JCommand readPrimConvert(AlignableDataInput in, JMethod method, JVMType from, JVMType to) {
		return new JCommand.Convert(from, to);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readLongCompare(AlignableDataInput in, JMethod method) {
		return new JCommand.SimpleCommand(SimpleCommands.LONG_COMPARE);
	}
	
	private static JCommand readConst(AlignableDataInput in, JMethod method, JVMType type, long value) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readLoadConstPool(AlignableDataInput in, JMethod method, boolean wide, int constSize) { // 2 for double/long ||| 1 for other types
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readLocalLoad(AlignableDataInput in, JMethod method, JVMType type, int index, boolean wide) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readLookupSwitch(AlignableDataInput in, JMethod method) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readLocalStore(AlignableDataInput in, JMethod method, JVMType reference, int index, boolean wide) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readPrimMath(AlignableDataInput in, JMethod method, JVMType type, JVMMath op) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readMonitorEnter(AlignableDataInput in, JMethod method) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readMonitorExit(AlignableDataInput in, JMethod method) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readMultiNewArray(AlignableDataInput in, JMethod method) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readNew(AlignableDataInput in, JMethod method) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	@SuppressWarnings("unused")
	private static JCommand readNOP(AlignableDataInput in, JMethod method) {
		return new JCommand.SimpleCommand(SimpleCommands.NOP);
	}
	
	private static JCommand readPOP(AlignableDataInput in, JMethod method, int pops) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readSetField(AlignableDataInput in, JMethod method) {
		return new JCommand.SimpleCommand(SimpleCommands.SET_FIELD);
	}
	
	private static JCommand readSetStaticField(AlignableDataInput in, JMethod method) {
		return new JCommand.SimpleCommand(SimpleCommands.SET_STATIC_FIELD);
	}
	
	@SuppressWarnings("unused")
	private static JCommand readRet(AlignableDataInput in, JMethod method, boolean wide) {
		throw new InternalError("I do not support ret, jsr, jsr_w, wide ret");
	}
	
	private static JCommand readReturn(AlignableDataInput in, JMethod method, JVMType type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readArrayLoad(AlignableDataInput in, JMethod method, JVMType type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readArrayStore(AlignableDataInput in, JMethod method, JVMType type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readPush(AlignableDataInput in, JMethod method, JVMType type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readSwap(AlignableDataInput in, JMethod method) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readTableSwitch(AlignableDataInput in, JMethod method) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	private static JCommand readWide(AlignableDataInput in, JMethod method) throws IOException {
		int b = in.readUnsignedByte();
		return switch (b) {
		// iload, fload, aload, lload, dload, istore, fstore, astore, lstore, dstore, or ret
		case 0x15 -> readLocalLoad(in, method, JVMType.INT, -1, true);
		case 0x17 -> readLocalLoad(in, method, JVMType.FLOAT, -1, true);
		case 0x19 -> readLocalLoad(in, method, JVMType.REFERENCE, -1, true);
		case 0x16 -> readLocalLoad(in, method, JVMType.LONG, -1, true);
		case 0x18 -> readLocalLoad(in, method, JVMType.DOUBLE, -1, true);
		case 0x36 -> readLocalStore(in, method, JVMType.INT, -1, true);
		case 0x38 -> readLocalStore(in, method, JVMType.FLOAT, -1, true);
		case 0x3A -> readLocalStore(in, method, JVMType.REFERENCE, -1, true);
		case 0x37 -> readLocalStore(in, method, JVMType.LONG, -1, true);
		case 0x39 -> readLocalStore(in, method, JVMType.DOUBLE, -1, true);
		case 0xA9 -> readRet(in, method, true);
		// iinc
		case 0x84 -> readIInc(in, method, true);
		default -> throw new ClassFormatError("unknown wide command: " + b + " : 0x" + Integer.toHexString(b));
		};
	}
	
	private static void readFields(AlignableDataInput in, ClassFile f) throws IOException {
		final int len    = in.readUnsignedShort();
		JField[]  fields = new JField[len];
		for (int i = 0; i < len; i++) {
			int       accessFlags  = in.readUnsignedShort();
			String    name         = f.cp(CPEntry.CPEUtf8.class, in.readUnsignedShort()).val();
			JType     type         = readType(f.cp(CPEntry.CPEUtf8.class, in.readUnsignedShort()).val(), false);
			Object    initialValue = null;
			final int alen         = in.readUnsignedShort();
			for (int ai = 0; ai < alen; ai++) {
				String aname = f.cp(CPEntry.CPEUtf8.class, in.readInt()).val();
				if ((accessFlags & Modifier.STATIC) != 0 && "ConstantValue".equals(aname)) {
					readInt(in, 2);
					CPEntry e = f.cp(CPEntry.class, in.readUnsignedShort());
					if (initialValue != null) {
						throw new ClassFormatError("multiple ConstantValue attributes for the same static field");
					}
					if (e instanceof CPEntry.CPEInt v) {
						initialValue = Integer.valueOf(v.val());
					} else if (e instanceof CPEntry.CPEFloat v) {
						initialValue = Float.valueOf(v.val());
					} else if (e instanceof CPEntry.CPELong v) {
						initialValue = Long.valueOf(v.val());
					} else if (e instanceof CPEntry.CPEDouble v) {
						initialValue = Double.valueOf(v.val());
					} else if (e instanceof CPEntry.CPEString v) {
						initialValue = v.str();
					} else {
						throw new ClassFormatError(
								"the class file contains a fild with an invalid constant value type: " + e.getClass().getSimpleName() + " : " + e);
					}
				} else {
					skipAttribute(in);
				}
			}
			fields[i] = new JField(accessFlags, name, type, initialValue);
		}
		f.init2(fields);
		throw new UnsupportedOperationException();
	}
	
	private static void skipAttribute(AlignableDataInput in) throws IOException {
		int skip = in.readInt();
		while (skip < 0) { // unsigned int, can iterate at most twice
			skipBytes(in, Integer.MAX_VALUE);
			skip -= Integer.MAX_VALUE;
		}
		skipBytes(in, skip);
	}
	
	private static void skipBytes(AlignableDataInput in, int skip) throws IOException {
		while (skip > 0) {
			skip -= in.skipBytes(skip);
			if (skip > 0) {
				in.readUnsignedByte();
				skip--;
			}
		}
	}
	
	private static void readInterfaces(AlignableDataInput in, ClassFile f) throws IOException {
		final int len  = in.readUnsignedShort();
		JType[]   ints = new JType[len];
		for (int i = 0; i < len; i++) {
			ints[i] = f.cp(CPEntry.CPEClass.class, in.readUnsignedShort()).type();
		}
		f.init1(ints);
	}
	
	private static void readAccessAndMyNames(AlignableDataInput in, ClassFile f) throws IOException {
		int   accessFlags     = in.readUnsignedShort();
		JType thisClass       = f.cp(CPEntry.CPEClass.class, in.readUnsignedShort()).type();
		int   superClassIndex = in.readUnsignedShort();
		JType superClass      = superClassIndex == 0 ? null : f.cp(CPEntry.CPEClass.class, superClassIndex).type();
		f.init0(accessFlags, thisClass, superClass);
	}
	
	private static ClassFile readConstantPool(AlignableDataInput in) throws IOException {
		int minor = in.readUnsignedShort();
		int major = in.readUnsignedShort();
		checkVersion(minor, major);
		int       cpCountP1 = in.readUnsignedShort();
		CPEntry[] entries   = new CPEntry[cpCountP1 - 1];
		for (int i = 1; i < cpCountP1; i++) {
			entries[i - 1] = readCPEntry(in, minor, major, entries, i);
		}
		ClassFile f = new ClassFile(minor, major, entries);
		for (int i = 1; i < cpCountP1; i++) {
			finishCPEntry(minor, major, entries[i - 1], f);
		}
		return f;
	}
	
	private static void finishCPEntry(int minor, int major, CPEntry cpe, ClassFile f) throws IOException {
		if (cpe instanceof CPEntry.CPEClass e) {
			CPEntry.CPEUtf8 u8 = f.cp(CPEntry.CPEUtf8.class, e.nameIndex);
			JType   t  = readType(u8.val(), false);
			e.initType(t);
		} else if (cpe instanceof CPEntry.CPEFieldRef fr) {
			CPEntry.CPEClass       t  = f.cp(CPEntry.CPEClass.class, fr.classIndex);
			CPEntry.CPENameAndType nt = f.cp(CPEntry.CPENameAndType.class, fr.nameAndTypeIndex);
			finishCPEntry(minor, major, nt, f);
			finishCPEntry(minor, major, t, f);
			fr.initVals(t.type(), nt.name(), nt.type());
		} else if (cpe instanceof CPEntry.CPEMethodRef mr) {
			CPEntry.CPEClass       t  = f.cp(CPEntry.CPEClass.class, mr.classIndex);
			CPEntry.CPENameAndType nt = f.cp(CPEntry.CPENameAndType.class, mr.nameAndTypeIndex);
			finishCPEntry(minor, major, nt, f);
			finishCPEntry(minor, major, t, f);
			mr.initVals(t.type(), nt.name(), nt.type());
		} else if (cpe instanceof CPEntry.CPEInterfaceMethodRef imr) {
			CPEntry.CPEClass       t  = f.cp(CPEntry.CPEClass.class, imr.classIndex);
			CPEntry.CPENameAndType nt = f.cp(CPEntry.CPENameAndType.class, imr.nameAndTypeIndex);
			finishCPEntry(minor, major, nt, f);
			finishCPEntry(minor, major, t, f);
			imr.initVals(t.type(), nt.name(), nt.type());
		} else if (cpe instanceof CPEntry.CPEMethodHandle e) {
			switch (e.refKind) {
			case 1, 2, 3, 4 -> {
				CPEntry.CPEFieldRef r = f.cp(CPEntry.CPEFieldRef.class, e.refIndex);
				finishCPEntry(minor, major, r, f);
				e.initField(r);
			}
			case 5, 8 -> {
				CPEntry.CPEMethodRef r = f.cp(CPEntry.CPEMethodRef.class, e.refIndex);
				finishCPEntry(minor, major, r, f);
				e.initMethod(r);
			}
			case 6, 7 -> {
				if (major < 52) {
					CPEntry.CPEMethodRef r = f.cp(CPEntry.CPEMethodRef.class, e.refIndex);
					finishCPEntry(minor, major, r, f);
					e.initMethod(r);
				} else {
					CPEntry r = f.cp(CPEntry.class, e.refIndex);
					finishCPEntry(minor, major, r, f);
					if (r instanceof CPEntry.CPEMethodRef m) {
						e.initMethod(m);
					} else if (r instanceof CPEntry.CPEInterfaceMethodRef im) {
						e.initInterfaceMethod(im);
					} else {
						throw new VerifyError("the index " + e.refIndex + " must be a method or interface method reference!");
					}
				}
			}
			case 9 -> {
				CPEntry.CPEInterfaceMethodRef r = f.cp(CPEntry.CPEInterfaceMethodRef.class, e.refIndex);
				finishCPEntry(minor, major, r, f);
				e.initInterfaceMethod(r);
			}
			default -> throw new AssertionError();
			}
		} else if (cpe instanceof CPEntry.CPEMethodType e) {
			e.initType(readMethodType(f.cp(CPEntry.CPEUtf8.class, e.mtypeIndex).val()));
		} else if (cpe instanceof CPEntry.CPEModule e) {
			e.initName(f.cp(CPEntry.CPEUtf8.class, e.nameIndex).val());
		} else if (cpe instanceof CPEntry.CPENameAndType e) {
			String name = f.cp(CPEntry.CPEUtf8.class, e.nameIndex).val();
			JType  type = readType(f.cp(CPEntry.CPEUtf8.class, e.typeIndex).val(), false);
			e.initVals(name, type);
		} else if (cpe instanceof CPEntry.CPEPackage e) {
			e.initName(f.cp(CPEntry.CPEUtf8.class, e.nameIndex).val());
		} else if (cpe instanceof CPEntry.CPEString e) {
			e.initStr(f.cp(CPEntry.CPEUtf8.class, e.nameIndex).val());
		}
	}
	
	private static CPEntry readCPEntry(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
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
	private static CPEntry readCPUtf8(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPEUtf8(in.readUTF());
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPInteger(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPEInt(in.readInt());
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPFloat(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPEFloat(in.readFloat());
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPLong(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPELong(in.readLong());
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPDouble(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		return new CPEntry.CPEDouble(in.readDouble());
	}
	
	private static CPEntry readCPClass(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int nameIndex = in.readUnsignedShort();
		if (nameIndex == 0 || nameIndex > entries.length) {
			throw new VerifyError(
					"the name index is outside of the allowed range: 1.." + entries.length + " invalid index = " + nameIndex + " current index = " + i + ")");
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
	
	private static CPEntry readCPString(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int nameIndex = in.readUnsignedShort();
		if (nameIndex == 0 || nameIndex > entries.length) {
			throw new VerifyError(
					"name index is outside of the allowed range: 1.." + entries.length + " invalid index: " + nameIndex + " the current index is: " + i + ")");
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
	
	private static CPEntry readCPFieldref(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int classIndex       = in.readUnsignedShort();
		int nameAndTypeIndex = in.readUnsignedShort();
		if (classIndex == 0 || classIndex > entries.length) {
			throw new VerifyError("class index is outside of the allowed range: 1.." + entries.length + " the invalid index is: " + classIndex
					+ " the current index is: " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError(
					"name_and_type index is outside of the allowed range: 1.." + entries.length + " invalid: " + nameAndTypeIndex + " current: " + i + ")");
		}
		return new CPEntry.CPEFieldRef(classIndex, nameAndTypeIndex);
	}
	
	private static CPEntry readCPMethodref(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int classIndex       = in.readUnsignedShort();
		int nameAndTypeIndex = in.readUnsignedShort();
		if (classIndex == 0 || classIndex > entries.length) {
			throw new VerifyError("the class index is outside of the allowed range: 1.." + entries.length + " invalid: " + classIndex + " current: " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError("name_and_type index is outside of the allowed range: 1.." + entries.length + " invalid index: " + nameAndTypeIndex
					+ " current index: " + i + ")");
		}
		return new CPEntry.CPEMethodRef(classIndex, nameAndTypeIndex);
	}
	
	private static CPEntry readCPInterfaceMethodref(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int classIndex       = in.readUnsignedShort();
		int nameAndTypeIndex = in.readUnsignedShort();
		if (classIndex == 0 || classIndex > entries.length) {
			throw new VerifyError(
					"the class index is outside of the allowed range: 1.." + entries.length + " invalid index = " + classIndex + " current index = " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError("the name_and_type index is outside of the permitted range: 1.." + entries.length + " invalid constant pool index: "
					+ nameAndTypeIndex + " current constant pool index is: " + i + ")");
		}
		return new CPEntry.CPEInterfaceMethodRef(classIndex, nameAndTypeIndex);
	}
	
	private static CPEntry readCPNameAndType(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV45_3(minor, major);
		int nameIndex = in.readUnsignedShort();
		int typeIndex = in.readUnsignedShort();
		if (nameIndex == 0 || nameIndex > entries.length) {
			throw new VerifyError(
					"the name index is outside of the allowed range: 1.." + entries.length + " cp index: " + nameIndex + " current cp index is: " + i + ")");
		}
		if (typeIndex == 0 || typeIndex > entries.length) {
			throw new VerifyError(
					"the type index is outside of the allowed range: 1.." + entries.length + " cp index: " + typeIndex + " current cp index is: " + i + ")");
		}
		return new CPEntry.CPENameAndType(nameIndex, typeIndex);
	}
	
	private static CPEntry readCPMethodHandle(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV51_0(minor, major);
		int refKind  = in.readUnsignedByte();
		int refIndex = in.readUnsignedShort();
		if (refKind == 0 || refKind > 9) {
			throw new VerifyError("refernece kind is invalid: " + refKind);
		}
		if (refIndex == 0 || refIndex > entries.length) {
			throw new VerifyError(
					"the reference index is outside of the allowed range: 1.." + entries.length + " cp-index: " + refIndex + " current cp-index is: " + i + ")");
		}
		return new CPEntry.CPEMethodHandle(refKind, refIndex);
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPMethodType(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV51_0(minor, major);
		int mtypeIndex = in.readUnsignedShort();
		return new CPEntry.CPEMethodType(mtypeIndex);
	}
	
	private static CPEntry readCPDynamic(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV55_0(minor, major);
		int bootstrapMetAttrIndex = in.readUnsignedShort();
		int nameAndTypeIndex      = in.readUnsignedShort();
		if (bootstrapMetAttrIndex == 0 || bootstrapMetAttrIndex > entries.length) {
			throw new VerifyError("the bootstrap_method_attr_name index is outside of the allowed range: 1.." + entries.length + " cp-index: "
					+ bootstrapMetAttrIndex + " current cp-index is: " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError("the name_and_type index is outside of the allowed range: 1.." + entries.length + " CP index: " + nameAndTypeIndex
					+ " current CP index is: " + i + ")");
		}
		return new CPEntry.CPEDynamic(bootstrapMetAttrIndex, nameAndTypeIndex);
	}
	
	private static CPEntry readCPInvokeDynamic(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV51_0(minor, major);
		int bootstrapMetAttrIndex = in.readUnsignedShort();
		int nameAndTypeIndex      = in.readUnsignedShort();
		if (bootstrapMetAttrIndex == 0 || bootstrapMetAttrIndex > entries.length) {
			throw new VerifyError("the bootstrap_method_attr_name index is outside of the allowed range: 1.." + entries.length + " index: " + bootstrapMetAttrIndex
					+ " current index is: " + i + ")");
		}
		if (nameAndTypeIndex == 0 || nameAndTypeIndex > entries.length) {
			throw new VerifyError("the name_and_type index is outside of the allowed range: 1.." + entries.length + " index: " + nameAndTypeIndex
					+ " current index is: " + i + ")");
		}
		return new CPEntry.CPEInvokeDynamic(bootstrapMetAttrIndex, nameAndTypeIndex);
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPModule(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV53_0(minor, major);
		int nameIndex = in.readUnsignedShort();
		return new CPEntry.CPEModule(nameIndex);
	}
	
	@SuppressWarnings("unused")
	private static CPEntry readCPPackage(AlignableDataInput in, int minor, int major, CPEntry[] entries, int i) throws IOException {
		checkV53_0(minor, major);
		int nameIndex = in.readUnsignedShort();
		return new CPEntry.CPEPackage(nameIndex);
	}
	
	private static MethodType readMethodType(String str) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
		AlignableDataInput   in   = new AlignableDataInput(bais);
		MethodType           mt   = readMethodType(in);
		if (bais.available() != 0) {
			throw new VerifyError("did not use the complete string to read the method descriptor! input: '" + str + "'");
		}
		return mt;
	}
	
	private static MethodType readMethodType(AlignableDataInput in) throws IOException {
		readByte(in, '(');
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
		AlignableDataInput   in   = new AlignableDataInput(baos);
		JType                res  = readType(in, allowVoid);
		if (baos.available() != 0) {
			throw new VerifyError("did not use the complete string to read the type! string: '" + str + "'");
		}
		return res;
	}
	
	private static JType readType(AlignableDataInput in, boolean allowVoid) throws IOException {
		int c = in.readUnsignedByte();
		return readType0(c, in, allowVoid);
	}
	
	private static JType readType0(int c, AlignableDataInput in, boolean allowVoid) throws IOException {
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
	
	private static void readInt(AlignableDataInput in, int i) throws IOException {
		int r = in.readInt();
		if (r != i) {
			throw new ClassFormatError("expected to read " + i + " but got " + r);
		}
	}
	
	private static void readShort(AlignableDataInput in, int s) throws IOException {
		int r = in.readUnsignedShort();
		if (r != s) {
			throw new ClassFormatError("expected to read " + s + " but got " + r);
		}
	}
	
	private static void readByte(AlignableDataInput in, int b) throws IOException {
		int r = in.readUnsignedByte();
		if (r != b) {
			throw new ClassFormatError(
					"expected to read " + (char) b + " : 0x" + Integer.toHexString(b) + " but got " + (char) r + " : 0x" + Integer.toHexString(r));
		}
	}
	
	// note that although there is some version and class file checking, the checks are far away from validation the class file
	
	private static void checkVn_n(int minMinor, int minMajor, String se, int minor, int major) {
		if (major < minMajor || (major == minMajor && minor < minMinor)) {
			throw new VerifyError("the class file uses a feature of a later version (needed " + minMajor + "." + minMinor + " (JavaSE " + se
					+ ")) but class file version is " + major + "." + minor);
		}
	}
	
	private static void checkV45_3(int minor, int major) {
		checkVn_n(3, 45, "1.0.2", minor, major);
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
