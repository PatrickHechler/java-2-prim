package de.hechler.patrick.j2p;

import java.lang.reflect.Modifier;
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
	private List<JCommand>      cmds;
	private JExceptionHandler[] handlers;
	private JStackMapEntry[]    stackMapEntries;
	
	public JMethod(ClassFile file, int accessFlags, String name, MethodType methodType) {
		this.file        = file;
		this.accessFlags = accessFlags;
		this.name        = name;
		this.methodType  = methodType;
	}
	
	public void initCode(int maxStack, int maxLocals) {
		if (this.cmds != null) {
			throw new ClassFormatError("multiple Code attribetes registerd!");
		}
		this.maxStack  = maxStack;
		this.maxLocals = maxLocals;
		this.cmds      = new ArrayList<>();
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
		if ((this.accessFlags & Modifier.STATIC) == 0) {
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
		entries[0] = new JStackMapEntry.FullDescribtion(-1, locals.toArray(new JSMEVerificationInfo[locals.size()]), JStackMapEntry.EMPTY_ARRAY);
		this.stackMapEntries = entries;
	}
	
	
	public void finish() {
		if (this.stackMapEntries == null) {
			initStackMapTable(new JStackMapEntry[1]);
		}
	}
	
	public int maxStack() {
		return this.maxStack;
	}
	
	public int maxLocals() {
		return this.maxLocals;
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
	
}

