package de.hechler.patrick.j2p.parse;

import java.lang.reflect.Modifier;
import java.util.Set;

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
	private JBootstrap[]    bootstrapMethods;
	private JType           nestHost;
	private Set<JType>      nestMembers;
	private Set<JType>      permittedSubclasses;
	private boolean         finish;
	
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
		notFinish();
		if (this.accessFlags != 0) {
			throw new AssertionError();
		}
		this.accessFlags = accessFlags;
		this.thisClass   = thisClass;
		this.superClass  = superClass;
	}
	
	public void init1(JType[] interfaces) {
		notFinish();
		if (this.interfaces != null) {
			throw new AssertionError();
		}
		this.interfaces = interfaces;
	}
	
	public void init2(JField[] fields) {
		notFinish();
		if (this.fields != null) {
			throw new AssertionError();
		}
		this.fields = fields;
	}
	
	public void init3(JMethod[] methods) {
		notFinish();
		if (this.methods != null) {
			throw new AssertionError();
		}
		this.methods = methods;
	}
	
	public void initBootstrapMethods(JBootstrap[] bootstrapMethods) {
		notFinish();
		if (this.bootstrapMethods != null) {
			throw new ClassFormatError("multiple BootstrapMethods attributes!");
		}
		this.bootstrapMethods = bootstrapMethods;
	}
	
	public void initNestHost(JType nestHost) {
		notFinish();
		if (this.nestHost != null) {
			throw new ClassFormatError("multiple NestHost attributes!");
		}
		this.nestHost = nestHost;
	}
	
	public void initNestMembers(JType[] nestMembers) {
		notFinish();
		if (this.nestMembers != null) {
			throw new ClassFormatError("multiple NestMembers attributes!");
		}
		this.nestMembers = Set.of(nestMembers);
	}
	
	public void initPermittedSubclasses(JType[] permittedSubclasses) {
		notFinish();
		if (this.permittedSubclasses != null) {
			throw new ClassFormatError("multiple PermittedSubclasses attributes!");
		}
		this.permittedSubclasses = Set.of(permittedSubclasses);
	}
	
	public void finish() {
		notFinish();
		this.finish = true;
	}
	
	private void notFinish() throws AssertionError {
		if (this.finish) {
			throw new AssertionError();
		}
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
	
	public JBootstrap bootstrapMethod(int index) {
		if (index < 0 || index >= this.bootstrapMethods.length) {
			throw new AssertionError();
		}
		return this.bootstrapMethods[index];
	}
	
	public JType nestHost() {
		return this.nestHost;
	}
	
	public Set<JType> nestMembers() {
		return this.nestMembers;
	}
	
	public Set<JType> permittedSubclasses() {
		return this.permittedSubclasses;
	}
	
	@Override
	public String toString() {
		String        nl = System.lineSeparator();
		StringBuilder b  = new StringBuilder();
		b.append("minor=").append(this.minor).append(" major=").append(this.major).append(nl);
		if (this.constantPool != null) {
			b.append("constant pool: [").append(nl);
			for (int i = 0; i < this.constantPool.length; i++) {
				b.append("  ").append(i + 1).append(" : 0x").append(Integer.toHexString(i + 1)).append(" = ").append(this.constantPool[i]).append(nl);
			}
			b.append(']').append(nl);
		}
		b.append("access_flags: ").append(this.accessFlags).append(" : 0x").append(Integer.toHexString(this.accessFlags)).append(" : ")
				.append(Modifier.toString(this.accessFlags)).append(nl);
		if (this.thisClass != null) {
			b.append("this_class: ").append(this.thisClass);
			if (this.superClass == null) b.append(nl);
			else b.append(" super_class: ").append(this.superClass).append(nl);
		} else if (this.superClass != null) {
			b.append("super_class: ").append(this.superClass).append(nl);
		}
		if (this.interfaces != null) {
			b.append("interfaces:");
			for (int i = 0; i < this.interfaces.length; i++) {
				b.append(' ').append(this.interfaces[i]);
			}
			b.append(nl);
		}
		if (this.fields != null) {
			b.append("fields:").append(nl);
			for (int i = 0; i < this.fields.length; i++) {
				b.append("  ").append(this.fields[i]).append(nl);
			}
		}
		if (this.methods != null) {
			b.append("methods:").append(nl);
			for (int i = 0; i < this.methods.length; i++) {
				this.methods[i].toString().lines().forEach(line -> b.append("  ").append(line).append(nl));
			}
		}
		if (this.bootstrapMethods != null) {
			b.append("BootstrapMethods:").append(nl);
			for (JBootstrap bm : this.bootstrapMethods) {
				b.append("  ").append(bm).append(nl);
			}
		}
		if (this.nestHost != null) {
			b.append("NestHost: ").append(this.nestHost);
		}
		if (this.nestMembers != null) {
			b.append("NestMembers:");
			for (JType nm : this.nestMembers) {
				b.append(' ').append(nm);
			}
			b.append(nl);
		}
		if (this.permittedSubclasses != null) {
			b.append("PermittedSubclasses:");
			for (JType ps : this.permittedSubclasses) {
				b.append(' ').append(ps);
			}
			b.append(nl);
		}
		
		return b.toString();
	}
	
}
