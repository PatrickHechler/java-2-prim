package de.hechler.patrick.j2p;


@SuppressWarnings("javadoc")
public sealed interface CPEntry {
	
	final class CPEClass implements CPEntry {
		
		public final int nameIndex;
		
		private JType type;
		
		public CPEClass(int nameIndex) { this.nameIndex = nameIndex; }
		
		public void initType(JType type) {
			if (this.type != null) {
				if (!this.type.equals(type)) {
					throw new AssertionError();
				}
			} else {
				this.type = type;
			}
		}
		
		public JType type() {
			JType t = this.type;
			if (t == null) throw new AssertionError();
			return t;
		}
		
	}
	
	final class CPEMethodType implements CPEntry {
		
		public final int mtypeIndex;
		
		private MethodType type;
		
		public CPEMethodType(int mtypeIndex) { this.mtypeIndex = mtypeIndex; }
		
		public void initType(MethodType type) {
			if (this.type != null) {
				if (!this.type.equals(type)) {
					throw new AssertionError();
				}
			} else {
				this.type = type;
			}
		}
		
		public MethodType type() {
			MethodType t = this.type;
			if (t == null) throw new AssertionError();
			return t;
		}
		
	}
	
	final class CPEFieldRef implements CPEntry {
		
		public final int classIndex;
		public final int nameAndTypeIndex;
		
		private JType  cls;
		private String name;
		private JType  type;
		
		public CPEFieldRef(int classIndex, int nameAndTypeIndex) { this.classIndex = classIndex; this.nameAndTypeIndex = nameAndTypeIndex; }
		
		
		public void initVals(JType cls, String name, JType type) {
			if (this.cls != null) {
				if (!this.cls.equals(cls) || !this.name.equals(name) || !this.type.equals(type)) {
					throw new AssertionError();
				}
			} else {
				this.cls  = cls;
				this.name = name;
				this.type = type;
			}
		}
		
		public JType cls() {
			JType c = this.cls;
			if (c == null) throw new AssertionError();
			return c;
		}
		
		public String name() {
			String n = this.name;
			if (n == null) throw new AssertionError();
			return n;
		}
		
		public JType type() {
			JType t = this.type;
			if (t == null) throw new AssertionError();
			return t;
		}
		
	}
	
	sealed class CPENormalMethodRef implements CPEntry {
		
		public final int classIndex;
		public final int nameAndTypeIndex;
		
		private JType  cls;
		private String name;
		private JType  type;
		
		public CPENormalMethodRef(int classIndex, int nameAndTypeIndex) { this.classIndex = classIndex; this.nameAndTypeIndex = nameAndTypeIndex; }
		
		public void initVals(JType cls, String name, JType type) {
			if (this.cls != null) {
				if (!this.cls.equals(cls) || !this.name.equals(name) || !this.type.equals(type)) {
					throw new AssertionError();
				}
			} else {
				this.cls  = cls;
				this.name = name;
				this.type = type;
			}
		}
		
		public JType cls() {
			JType c = this.cls;
			if (c == null) throw new AssertionError();
			return c;
		}
		
		public String name() {
			String n = this.name;
			if (n == null) throw new AssertionError();
			return n;
		}
		
		public JType type() {
			JType t = this.type;
			if (t == null) throw new AssertionError();
			return t;
		}
		
	}
	
	final class CPEMethodRef extends CPENormalMethodRef {
		
		public CPEMethodRef(int classIndex, int nameAndTypeIndex) {
			super(classIndex, nameAndTypeIndex);
		}
		
	}
	
	final class CPEInterfaceMethodRef extends CPENormalMethodRef {
		
		public CPEInterfaceMethodRef(int classIndex, int nameAndTypeIndex) {
			super(classIndex, nameAndTypeIndex);
		}
		
	}
	
	final class CPEString implements CPEntry {
		
		public final int nameIndex;
		
		private String str;
		
		public CPEString(int nameIndex) { this.nameIndex = nameIndex; }
		
		public void initStr(String str) {
			if (this.str != null) {
				if (!this.str.equals(str)) {
					throw new AssertionError();
				}
			} else {
				this.str = str;
			}
		}
		
		public String str() {
			String s = this.str;
			if (s == null) throw new AssertionError();
			return s;
		}
		
	}
	
	record CPEInt(int val) implements CPEntry {}
	
	record CPEFloat(float val) implements CPEntry {}
	
	record CPELong(long val) implements CPEntry {}
	
	record CPEDouble(double val) implements CPEntry {}
	
	final class CPENameAndType implements CPEntry {
		
		public final int nameIndex;
		public final int typeIndex;
		
		private String name;
		private JType  type;
		
		public CPENameAndType(int nameIndex, int typeIndex) { this.nameIndex = nameIndex; this.typeIndex = typeIndex; }
		
		public void initVals(String name, JType type) {
			if (this.name != null) {
				if (!this.name.equals(name) || !this.type.equals(type)) {
					throw new AssertionError();
				}
			} else {
				this.name = name;
				this.type = type;
			}
		}
		
		public String name() {
			String n = this.name;
			if (n == null) throw new AssertionError();
			return n;
		}
		
		public JType type() {
			JType t = this.type;
			if (t == null) throw new AssertionError();
			return t;
		}
		
	}
	
	record CPEUtf8(String val) implements CPEntry {}
	
	final class CPEMethodHandle implements CPEntry {
		
		public final int refKind;
		public final int refIndex;
		
		private CPEFieldRef           fref;
		private CPEMethodRef          mref;
		private CPEInterfaceMethodRef imref;
		
		public CPEMethodHandle(int refKind, int refIndex) { this.refKind = refKind; this.refIndex = refIndex; }
		
		public void initField(CPEFieldRef f) {
			if (this.fref != null) {
				if (!this.fref.equals(f)) {
					throw new AssertionError();
				}
			} else {
				if (this.refKind > 4) {
					throw new AssertionError();
				}
				this.fref = f;
			}
		}
		
		public void initMethod(CPEMethodRef m) {
			if (this.mref != null) {
				if (!this.mref.equals(m)) {
					throw new AssertionError();
				}
			} else if (this.imref != null) {
				throw new AssertionError();
			} else {
				if (this.refKind < 4 || this.refKind > 8) {
					throw new AssertionError();
				}
				this.mref = m;
			}
		}
		
		public void initInterfaceMethod(CPEInterfaceMethodRef im) {
			if (this.imref != null) {
				if (!this.imref.equals(im)) {
					throw new AssertionError();
				}
			} else if (this.mref != null) {
				throw new AssertionError();
			} else {
				if (this.refKind < 5 || this.refKind == 8) {
					throw new AssertionError();
				}
				this.imref = im;
			}
		}
		
		public CPEFieldRef fieldRef() {
			return this.fref;
		}
		
		public CPEInterfaceMethodRef interfaceMethodRef() {
			return this.imref;
		}
		
		public CPEMethodRef methodRef() {
			return this.mref;
		}
		
	}
	
	record CPEDynamic(int bootstrapMetAttrIndex, int nameAndTypeIndex) implements CPEntry {}
	
	record CPEInvokeDynamic(int bootstrapMetAttrIndex, int nameAndTypeIndex) implements CPEntry {}
	
	final class CPEModule implements CPEntry {
		
		public final int nameIndex;
		
		private String name;
		
		public CPEModule(int nameIndex) { this.nameIndex = nameIndex; }
		
		public void initName(String name) {
			if (this.name != null) {
				if (!this.name.equals(name)) {
					throw new AssertionError();
				}
			} else {
				this.name = name;
			}
		}
		
		public String name() {
			String n = this.name;
			if (n == null) throw new AssertionError();
			return n;
		}
		
	}
	
	final class CPEPackage implements CPEntry {
		
		public final int nameIndex;
		
		private String name;
		
		public CPEPackage(int nameIndex) { this.nameIndex = nameIndex; }
		
		public void initName(String name) {
			if (this.name != null) {
				if (!this.name.equals(name)) {
					throw new AssertionError();
				}
			} else {
				this.name = name;
			}
		}
		
		public String name() {
			String n = this.name;
			if (n == null) throw new AssertionError();
			return n;
		}
		
	}
	
}
