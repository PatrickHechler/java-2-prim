package de.hechler.patrick.j2p.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("javadoc")
public class JMethod {
	
	public final ClassFile      file;
	public final int            accessFlags;
	public final String         name;
	public final MethodType     methodType;
	private int                 maxStack;
	private int                 maxLocals;
	private int                 codeLength;
	private List<JCommand>      cmds;
	private JExceptionHandler[] handlers;
	private JStackMapEntry[]    stackMapEntries;
	
	public JMethod(ClassFile file, int accessFlags, String name, MethodType methodType) {
		this.file        = file;
		this.accessFlags = accessFlags;
		this.name        = name;
		this.methodType  = methodType;
	}
	
	public void initCode(int maxStack, int maxLocals, int codeLength) {
		if (this.cmds != null) {
			throw new ClassFormatError("multiple Code attribetes registerd!");
		}
		this.maxStack   = maxStack;
		this.maxLocals  = maxLocals;
		this.codeLength = codeLength;
		this.cmds       = new ArrayList<>();
	}
	
	public void addCommand(JCommand cmd) {
		this.cmds.add(cmd);
	}
	
	public void initHandlers(JExceptionHandler[] handlers) {
		if (this.handlers != null) {
			throw new AssertionError();
		}
		this.cmds     = Collections.unmodifiableList(this.cmds);
		this.handlers = handlers;
	}
	
	public void initStackMapTable(JStackMapEntry[] entries) {
		if (this.handlers == null || this.stackMapEntries != null) {
			throw new AssertionError();
		}
		List<JSMEVerificationInfo> locals = new ArrayList<>();
		if ((this.accessFlags & Modifier.ACC_STATIC) == 0) {
			if ("<init>".equals(this.name)) {
				locals.add(JSMEVerificationInfo.SimpleInfo.UNINITIALIZEDTHIS);
			} else {
				locals.add(new JSMEVerificationInfo.ObjectInfo(this.file.thisClass()));
			}
		}
		for (JType type : this.methodType.params()) {
			if (type instanceof JType.JPrimType p) {
				switch (p) {
				case BOOLEAN, BYTE, CHAR, INT, SHORT -> locals.add(JSMEVerificationInfo.SimpleInfo.INTEGER);
				case FLOAT -> locals.add(JSMEVerificationInfo.SimpleInfo.FLOAT);
				case LONG -> {
					locals.add(JSMEVerificationInfo.SimpleInfo.LONG);
					locals.add(JSMEVerificationInfo.SimpleInfo.TOP);
				}
				case DOUBLE -> {
					locals.add(JSMEVerificationInfo.SimpleInfo.DOUBLE);
					locals.add(JSMEVerificationInfo.SimpleInfo.TOP);
				}
				case VOID -> throw new AssertionError();
				default -> throw new AssertionError();
				}
			} else {
				locals.add(new JSMEVerificationInfo.ObjectInfo(type));
			}
		}
		entries[0]           = new JStackMapEntry.FullDescribtion(-1, locals.toArray(new JSMEVerificationInfo[locals.size()]), JStackMapEntry.EMPTY_ARRAY);
		this.stackMapEntries = entries;
	}
	
	
	public void finish() {
		if (this.stackMapEntries == null && (this.accessFlags & Modifier.ACC_ABSTRACT) == 0) {
			initStackMapTable(new JStackMapEntry[1]);
		}
	}
	
	public int maxStack() {
		return this.maxStack;
	}
	
	public int maxLocals() {
		return this.maxLocals;
	}
	
	
	public int codeLength() {
		return this.codeLength;
	}
	
	public List<JCommand> commands() {
		return this.cmds;
	}
	
	public JExceptionHandler[] handlers() {
		return this.handlers;
	}
	
	public JStackMapEntry[] stackMapEntries() {
		return this.stackMapEntries;
	}
	
	@Override
	public String toString() {
		String        nl = System.lineSeparator();
		StringBuilder b  = new StringBuilder();
		b.append("accessFlags: ").append(this.accessFlags).append(" : 0x").append(Integer.toHexString(this.accessFlags)).append(" : ")
				.append(Modifier.methodString(this.accessFlags));
		if (this.name != null) {
			b.append(" name=").append(this.name);
		}
		if (this.methodType != null) {
			b.append(" descriptor=").append(this.methodType);
		}
		b.append(nl);
		b.append("  maxLocals=").append(this.maxLocals).append(" maxStack=").append(this.maxStack).append(nl);
		if (this.cmds != null) {
			b.append("  Code: [").append(nl);
			for (JCommand c : this.cmds) {
				b.append("    at: ").append(c.address()).append(" : 0x").append(Long.toHexString(c.address())).append(" ==> ").append(c).append(nl);
			}
			b.append("  ]").append(nl);
		}
		if (this.handlers != null) {
			b.append("  Code->ExceptionHandlers: [").append(nl);
			for (int i = 0; i < this.handlers.length; i++) {
				b.append("    ").append(this.handlers[i]).append(nl);
			}
			b.append("  ]").append(nl);
		}
		if (this.stackMapEntries != null) {
			b.append("  Code->StackMapTable: [").append(nl);
			b.append("    implicit first entry: ").append(nl);
			for (int i = 0; i < this.stackMapEntries.length; i++) {
				b.append("    ").append(this.stackMapEntries[i]).append(nl);
			}
			b.append("  ]").append(nl);
		}
		return b.toString();
	}
	
}

