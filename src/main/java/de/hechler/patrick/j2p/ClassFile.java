package de.hechler.patrick.j2p;


@SuppressWarnings("javadoc")
public class ClassFile {
	
	public final int        minor;
	public final int        major;
	private final CPEntry[] constantPool;
	private int             accessFlags;
	private JType           thisClass;
	private JType           superClass;
	private JType[]         interfaces;
	private JField[]        fields;
	private JMethod[]       methods;
	
	public ClassFile(int minor, int major, CPEntry[] constantPool) {
		this.minor        = minor;
		this.major        = major;
		this.constantPool = constantPool;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends CPEntry> T cp(Class<T> cls, int index) {
		if (index == 0 || index > this.constantPool.length) {
			throw new VerifyError("invalid constant pool index: " + index);
		}
		CPEntry e = this.constantPool[index - 1];
		if (cls.isInstance(e)) {
			return (T) e;
		}
		throw new VerifyError("invalid constant pool index: " + index + "! the reference must be of type: " + cls.getSimpleName());
	}
	
	public void init0(int accessFlags, JType thisClass, JType superClass) {
		if (this.accessFlags != 0) {
			throw new AssertionError();
		}
		this.accessFlags = accessFlags;
		this.thisClass   = thisClass;
		this.superClass  = superClass;
	}
	
	public void init1(JType[] interfaces) {
		if (this.interfaces != null) {
			throw new AssertionError();
		}
		this.interfaces = interfaces;
	}

	public void init2(JField[] fields) {
		if (this.fields != null) {
			throw new AssertionError();
		}
		this.fields = fields;
	}
	
	public void init3(JMethod[] methods) {
		if (this.methods != null) {
			throw new AssertionError();
		}
		this.methods = methods;
	}
	
	public CPEntry[] gonstantPool() {
		return this.constantPool;
	}
	
	public int accessFlags() {
		return this.accessFlags;
	}
	
	public JType thisClass() {
		return this.thisClass;
	}
	
	public JType superClass() {
		return this.superClass;
	}
	
	public JType[] interfaces() {
		return this.interfaces;
	}
	
	public JField[] fields() {
		return this.fields;
	}
	
}
