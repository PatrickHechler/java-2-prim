package de.hechler.patrick.j2p.parse;

import java.lang.StackWalker.Option;
import java.util.Arrays;

import de.hechler.patrick.j2p.parse.CPEntry.CPEFieldRef;
import de.hechler.patrick.j2p.parse.ClassReader.JVMCmp;
import de.hechler.patrick.j2p.parse.ClassReader.JVMMath;
import de.hechler.patrick.j2p.parse.ClassReader.JVMType;

@SuppressWarnings("javadoc")
public abstract sealed class JCommand {
	
	private long addr = -1L;
	
	public void initAdress(long addr) {
		if (this.addr != -1L) {
			throw new AssertionError();
		}
		this.addr = addr;
	}
	
	public long address() {
		if (this.addr == -1L) {
			Class<?> caller = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass();
			if (caller != this.getClass() && caller != this.getClass().getSuperclass()) {
				throw new AssertionError();
			}
		}
		return this.addr;
	}
	
	@Override
	public int hashCode() {
		final int prime  = 31;
		int       result = 1;
		result = prime * result + (int) (this.addr ^ (this.addr >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JCommand other = (JCommand) obj;
		if (this.addr != other.addr) return false;
		return true;
	}
	
	
	@Override
	public abstract String toString();
	
	public static final class NewArray extends JCommand {
		
		public final JType componentType;
		
		public NewArray(JType componentType) {
			this.componentType = componentType;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.componentType == null) ? 0 : this.componentType.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			NewArray other = (NewArray) obj;
			if (this.componentType == null) {
				if (other.componentType != null) return false;
			} else if (!this.componentType.equals(other.componentType)) return false;
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("NewArray [componentType=");
			builder.append(this.componentType);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static sealed class InvokeNormal extends JCommand {
		
		public final CPEntry.CPENormalMethodRef invoke;
		
		public InvokeNormal(CPEntry.CPENormalMethodRef invoke) {
			this.invoke = invoke;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.invoke == null) ? 0 : this.invoke.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			InvokeNormal other = (InvokeNormal) obj;
			if (this.invoke == null) {
				if (other.invoke != null) return false;
			} else if (!this.invoke.equals(other.invoke)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.getClass().getSimpleName()).append(" [invoke=");
			builder.append(this.invoke);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class InvokeInterface extends InvokeNormal {
		
		public InvokeInterface(CPEntry.CPEInterfaceMethodRef invoke) {
			super(invoke);
		}
		
	}
	
	public static final class InvokeSpecial extends InvokeNormal {
		
		public InvokeSpecial(CPEntry.CPENormalMethodRef invoke) {
			super(invoke);
		}
		
	}
	
	public static final class InvokeStatic extends InvokeNormal {
		
		public InvokeStatic(CPEntry.CPENormalMethodRef invoke) {
			super(invoke);
		}
		
	}
	
	public static final class InvokeVirtual extends InvokeNormal {
		
		public InvokeVirtual(CPEntry.CPEMethodRef invoke) {
			super(invoke);
		}
		
	}
	
	public static final class InvokeDynamic extends JCommand {
		
		public final CPEntry.CPEInvokeDynamic invoke;
		
		public InvokeDynamic(CPEntry.CPEInvokeDynamic invoke) {
			this.invoke = invoke;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.invoke == null) ? 0 : this.invoke.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			InvokeDynamic other = (InvokeDynamic) obj;
			if (this.invoke == null) {
				if (other.invoke != null) return false;
			} else if (!this.invoke.equals(other.invoke)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("InvokeDynamic [invoke=");
			builder.append(this.invoke);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class FPCompare extends JCommand {
		
		public final JVMType type;
		public final int     nanValue;
		
		public FPCompare(JVMType type, int nanValue) {
			this.type     = type;
			this.nanValue = nanValue;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.nanValue;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			FPCompare other = (FPCompare) obj;
			if (this.nanValue != other.nanValue) return false;
			if (this.type != other.type) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FPCompare [type=");
			builder.append(this.type);
			builder.append(", nanValue=");
			builder.append(this.nanValue);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class StackDup extends JCommand {
		
		public final int skip;
		public final int dupCnt;
		
		public StackDup(int skip, int dupCnt) {
			this.skip   = skip;
			this.dupCnt = dupCnt;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = super.hashCode();
			result = prime * result + this.dupCnt;
			result = prime * result + this.skip;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			StackDup other = (StackDup) obj;
			if (this.dupCnt != other.dupCnt) return false;
			if (this.skip != other.skip) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("StackDup [skip=");
			builder.append(this.skip);
			builder.append(", dupCnt=");
			builder.append(this.dupCnt);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public abstract static sealed class ActField extends JCommand {
		
		public final boolean     instance;
		public final CPEFieldRef field;
		
		public ActField(boolean instance, CPEntry.CPEFieldRef field) {
			this.instance = instance;
			this.field    = field;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.field == null) ? 0 : this.field.hashCode());
			result = prime * result + (this.instance ? 1231 : 1237);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			ActField other = (ActField) obj;
			if (this.field == null) {
				if (other.field != null) return false;
			} else if (!this.field.equals(other.field)) return false;
			if (this.instance != other.instance) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.getClass().getSimpleName()).append(" [instance=");
			builder.append(this.instance);
			builder.append(", field=");
			builder.append(this.field);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class GetField extends ActField {
		
		public GetField(boolean instance, CPEFieldRef field) {
			super(instance, field);
		}
		
	}
	
	public static final class PutField extends ActField {
		
		public PutField(boolean instance, CPEFieldRef field) {
			super(instance, field);
		}
		
	}
	
	public static final class IInc extends JCommand {
		
		public final int localVarIndex;
		public final int addConst;
		
		public IInc(int localVarIndex, int addConst) {
			this.localVarIndex = localVarIndex;
			this.addConst      = addConst;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.addConst;
			result = prime * result + this.localVarIndex;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			IInc other = (IInc) obj;
			if (this.addConst != other.addConst) return false;
			if (this.localVarIndex != other.localVarIndex) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("IInc [localVarIndex=");
			builder.append(this.localVarIndex);
			builder.append(", addConst=");
			builder.append(this.addConst);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class LookupSwitch extends JCommand {
		
		public final int   defaultOffset;
		public final int[] matches;
		public final int[] offsets;
		
		public LookupSwitch(int defaultOffset, int[] matches, int[] offsets) {
			this.defaultOffset = defaultOffset;
			this.matches       = matches;
			this.offsets       = offsets;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.defaultOffset;
			result = prime * result + Arrays.hashCode(this.matches);
			result = prime * result + Arrays.hashCode(this.offsets);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			LookupSwitch other = (LookupSwitch) obj;
			if (this.defaultOffset != other.defaultOffset) return false;
			if (!Arrays.equals(this.matches, other.matches)) return false;
			if (!Arrays.equals(this.offsets, other.offsets)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LookupSwitch [defaultOffset=");
			builder.append(this.defaultOffset);
			builder.append(", matches=");
			builder.append(Arrays.toString(this.matches));
			builder.append(", offsets=");
			builder.append(Arrays.toString(this.offsets));
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class TableSwitch extends JCommand {
		
		public final int   defaultOffset;
		public final int   minValue;
		public final int   maxValue;
		public final int[] offsets;
		
		public TableSwitch(int defaultOffset, int minValue, int maxValue, int[] offsets) {
			this.defaultOffset = defaultOffset;
			this.minValue      = minValue;
			this.maxValue      = maxValue;
			this.offsets       = offsets;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.defaultOffset;
			result = prime * result + this.maxValue;
			result = prime * result + this.minValue;
			result = prime * result + Arrays.hashCode(this.offsets);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			TableSwitch other = (TableSwitch) obj;
			if (this.defaultOffset != other.defaultOffset) return false;
			if (this.maxValue != other.maxValue) return false;
			if (this.minValue != other.minValue) return false;
			if (!Arrays.equals(this.offsets, other.offsets)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("TableSwitch [defaultOffset=");
			builder.append(this.defaultOffset);
			builder.append(", minValue=");
			builder.append(this.minValue);
			builder.append(", maxValue=");
			builder.append(this.maxValue);
			builder.append(", offsets=");
			builder.append(Arrays.toString(this.offsets));
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class Convert extends JCommand {
		
		public final JVMType from;
		public final JVMType to;
		
		public Convert(JVMType from, JVMType to) {
			this.from = from;
			this.to   = to;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.from == null) ? 0 : this.from.hashCode());
			result = prime * result + ((this.to == null) ? 0 : this.to.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			Convert other = (Convert) obj;
			if (this.from != other.from) return false;
			if (this.to != other.to) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Convert [from=");
			builder.append(this.from);
			builder.append(", to=");
			builder.append(this.to);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class CheckCast extends JCommand {
		
		public final JType   type;
		public final boolean fail;
		
		public CheckCast(JType type, boolean fail) {
			this.type = type;
			this.fail = fail;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + (this.fail ? 1231 : 1237);
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			CheckCast other = (CheckCast) obj;
			if (this.fail != other.fail) return false;
			if (this.type == null) {
				if (other.type != null) return false;
			} else if (!this.type.equals(other.type)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("CheckCast [type=");
			builder.append(this.type);
			builder.append(", fail=");
			builder.append(this.fail);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class Const extends JCommand {
		
		public final JVMType type;
		public final long    value;
		
		public Const(JVMType type, long value) {
			this.type  = type;
			this.value = value;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			result = prime * result + (int) (this.value ^ (this.value >>> 32));
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			Const other = (Const) obj;
			if (this.type != other.type) return false;
			if (this.value != other.value) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Const [type=");
			builder.append(this.type);
			builder.append(", value=");
			builder.append(this.value);
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class LocalVariableLoad extends JCommand {
		
		public final JVMType type;
		public final int     index;
		
		public LocalVariableLoad(JVMType type, int index) {
			this.type  = type;
			this.index = index;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.index;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			LocalVariableLoad other = (LocalVariableLoad) obj;
			if (this.index != other.index) return false;
			if (this.type != other.type) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LocalVariableLoad [type=");
			builder.append(this.type);
			builder.append(", index=");
			builder.append(this.index);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class LocalVariableStore extends JCommand {
		
		public final JVMType type;
		public final int     index;
		
		public LocalVariableStore(JVMType type, int index) {
			this.type  = type;
			this.index = index;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.index;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			LocalVariableStore other = (LocalVariableStore) obj;
			if (this.index != other.index) return false;
			if (this.type != other.type) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LocalVariableStore [type=");
			builder.append(this.type);
			builder.append(", index=");
			builder.append(this.index);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class PrimMath extends JCommand {
		
		public final JVMType type;
		public final JVMMath op;
		
		public PrimMath(JVMType type, JVMMath op) {
			this.type = type;
			this.op   = op;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.op == null) ? 0 : this.op.hashCode());
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			PrimMath other = (PrimMath) obj;
			if (this.op != other.op) return false;
			if (this.type != other.type) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("PrimMath [type=");
			builder.append(this.type);
			builder.append(", op=");
			builder.append(this.op);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class MultiNewArray extends JCommand {
		
		public final JType.ArrayType type;
		public final int             dimensions;
		
		public MultiNewArray(JType.ArrayType type, int dimensions) {
			this.type       = type;
			this.dimensions = dimensions;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.dimensions;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			MultiNewArray other = (MultiNewArray) obj;
			if (this.dimensions != other.dimensions) return false;
			if (this.type == null) {
				if (other.type != null) return false;
			} else if (!this.type.equals(other.type)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MultiNewArray [type=");
			builder.append(this.type);
			builder.append(", dimensions=");
			builder.append(this.dimensions);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class Push extends JCommand {
		
		public final JVMType type;
		public final int     value;
		
		public Push(JVMType type, int value) {
			this.type  = type;
			this.value = value;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			result = prime * result + this.value;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			Push other = (Push) obj;
			if (this.type != other.type) return false;
			if (this.value != other.value) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Push [type=");
			builder.append(this.type);
			builder.append(", value=");
			builder.append(this.value);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class Pop extends JCommand {
		
		public final int pops;
		
		public Pop(int pops) {
			this.pops = pops;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.pops;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			Pop other = (Pop) obj;
			if (this.pops != other.pops) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Pop [pops=");
			builder.append(this.pops);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class New extends JCommand {
		
		public final JType type;
		
		public New(JType type) {
			this.type = type;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			New other = (New) obj;
			if (this.type == null) {
				if (other.type != null) return false;
			} else if (!this.type.equals(other.type)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("New [type=");
			builder.append(this.type);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class Return extends JCommand {
		
		public final JVMType type;
		
		public Return(JVMType type) {
			this.type = type;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			Return other = (Return) obj;
			if (this.type != other.type) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Return [type=");
			builder.append(this.type);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class ArrayLoad extends JCommand {
		
		public final JVMType type;
		
		public ArrayLoad(JVMType type) {
			this.type = type;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			ArrayLoad other = (ArrayLoad) obj;
			if (this.type != other.type) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ArrayLoad [type=");
			builder.append(this.type);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class ArrayStore extends JCommand {
		
		public final JVMType type;
		
		public ArrayStore(JVMType type) {
			this.type = type;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			ArrayStore other = (ArrayStore) obj;
			if (this.type != other.type) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ArrayStore [type=");
			builder.append(this.type);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class LoadConstPool extends JCommand {
		
		public final CPEntry entry;
		
		public LoadConstPool(CPEntry entry) {
			this.entry = entry;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.entry == null) ? 0 : this.entry.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			LoadConstPool other = (LoadConstPool) obj;
			if (this.entry == null) {
				if (other.entry != null) return false;
			} else if (!this.entry.equals(other.entry)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LoadConstPool [entry=");
			builder.append(this.entry);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static sealed class Goto extends JCommand {
		
		public final int relativeAdress;
		
		public Goto(int relativeAdress) {
			this.relativeAdress = relativeAdress;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.relativeAdress;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			Goto other = (Goto) obj;
			if (this.relativeAdress != other.relativeAdress) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Goto [relativeAdress=");
			builder.append(this.relativeAdress).append(" : 0x").append(Integer.toHexString(this.relativeAdress));
			builder.append(", calculatedTarget=");
			builder.append(this.relativeAdress + super.address()).append(" : 0x").append(Long.toHexString(this.relativeAdress + super.address()));
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
		public int targetAddress() {
			return (int) (super.addr + this.relativeAdress);
		}
		
	}
	
	public static sealed class SignCheck extends Goto {
		
		public final JVMType type;
		public final JVMCmp  cmp;
		
		public SignCheck(int relativeAdress, JVMType type, JVMCmp cmp) {
			super(relativeAdress);
			this.type = type;
			this.cmp  = cmp;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = super.hashCode();
			result = prime * result + ((this.cmp == null) ? 0 : this.cmp.hashCode());
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			if (!(obj instanceof SignCheck)) return false;
			SignCheck other = (SignCheck) obj;
			if (this.cmp != other.cmp) return false;
			if (this.type != other.type) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.getClass().getSimpleName()).append(" [type=");
			builder.append(this.type);
			builder.append(", cmp=");
			builder.append(this.cmp);
			builder.append(", relativeTarget=");
			builder.append(super.relativeAdress).append(" : 0x").append(Integer.toHexString(super.relativeAdress));
			builder.append(", calculatedTarget=");
			builder.append(super.relativeAdress + super.address()).append(" : 0x").append(Long.toHexString(super.relativeAdress + super.address()));
			builder.append(", address()=");
			builder.append(this.address());
			builder.append(" : 0x");
			builder.append(Integer.toHexString((int) this.address()));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class Compare extends SignCheck {
		
		public Compare(int relativeAdress, JVMType type, JVMCmp cmp) {
			super(relativeAdress, type, cmp);
		}
		
	}
	
	public enum SimpleCommands {
		// synchronizing
		MONITOR_ENTER, MONITOR_EXIT,
		// miscellaneous
		ARRAY_LENGTH, A_THROW, LONG_COMPARE, NOP, SWAP,
	
	}
	
	public static final class SimpleCommand extends JCommand {
		
		public final SimpleCommands commandType;
		
		public SimpleCommand(SimpleCommands commandType) {
			this.commandType = commandType;
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.commandType == null) ? 0 : this.commandType.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			if (!(obj instanceof SimpleCommand)) return false;
			SimpleCommand other = (SimpleCommand) obj;
			if (this.commandType != other.commandType) return false;
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("SimpleCommand [commandType=");
			builder.append(this.commandType);
			builder.append(", address()=");
			builder.append(this.address());
			builder.append("]");
			return builder.toString();
		}
		
	}
	
}
