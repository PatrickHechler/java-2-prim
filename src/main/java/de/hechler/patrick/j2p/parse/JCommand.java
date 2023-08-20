package de.hechler.patrick.j2p.parse;

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
			throw new AssertionError();
		}
		return this.addr;
	}
	
	@Override
	public abstract String toString();
	
	public static final class NewArray extends JCommand {
		
		public final JType componentType;
		
		public NewArray(JType componentType) {
			this.componentType = componentType;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("NewArray [componentType=");
			builder.append(this.componentType);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.getClass().getSimpleName()).append(" [invoke=");
			builder.append(this.invoke);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("InvokeDynamic [invoke=");
			builder.append(this.invoke);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FPCompare [type=");
			builder.append(this.type);
			builder.append(", nanValue=");
			builder.append(this.nanValue);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("StackDup [skip=");
			builder.append(this.skip);
			builder.append(", dupCnt=");
			builder.append(this.dupCnt);
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static sealed class ActField extends JCommand {
		
		public final boolean     instance;
		public final CPEFieldRef field;
		
		public ActField(boolean instance, CPEntry.CPEFieldRef field) {
			this.instance = instance;
			this.field    = field;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.getClass().getSimpleName()).append(" [instance=");
			builder.append(this.instance);
			builder.append(", field=");
			builder.append(this.field);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("IInc [localVarIndex=");
			builder.append(this.localVarIndex);
			builder.append(", addConst=");
			builder.append(this.addConst);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LookupSwitch [defaultOffset=");
			builder.append(this.defaultOffset);
			builder.append(", matches=");
			builder.append(Arrays.toString(this.matches));
			builder.append(", offsets=");
			builder.append(Arrays.toString(this.offsets));
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Convert [from=");
			builder.append(this.from);
			builder.append(", to=");
			builder.append(this.to);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("CheckCast [type=");
			builder.append(this.type);
			builder.append(", fail=");
			builder.append(this.fail);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LocalVariableLoad [type=");
			builder.append(this.type);
			builder.append(", index=");
			builder.append(this.index);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LocalVariableStore [type=");
			builder.append(this.type);
			builder.append(", index=");
			builder.append(this.index);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("PrimMath [type=");
			builder.append(this.type);
			builder.append(", op=");
			builder.append(this.op);
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class MultiNewArray extends JCommand {
		
		public final JType type;
		public final int   dimensions;
		
		public MultiNewArray(JType type, int dimensions) {
			this.type       = type;
			this.dimensions = dimensions;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MultiNewArray [type=");
			builder.append(this.type);
			builder.append(", dimensions=");
			builder.append(this.dimensions);
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static final class Push extends JCommand {
		
		public final int bytes;
		public final int value;
		
		public Push(int bytes, int value) {
			this.bytes = bytes;
			this.value = value;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Push [size (in bytes)=");
			builder.append(this.bytes);
			builder.append(", value=");
			builder.append(this.value);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("New [type=");
			builder.append(this.type);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Return [type=");
			builder.append(this.type);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ArrayLoad [type=");
			builder.append(this.type);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ArrayStore [type=");
			builder.append(this.type);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LoadConstPool [entry=");
			builder.append(this.entry);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Goto [relativeAdress=");
			builder.append(this.relativeAdress).append(" : 0x").append(Integer.toHexString(this.relativeAdress));
			builder.append(", calculatedTarget=");
			builder.append(this.relativeAdress + super.address()).append(" : 0x").append(Long.toHexString(this.relativeAdress + super.address()));
			builder.append("]");
			return builder.toString();
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
		// class checking
		CHECK_CASTS, INSTANCE_OF,
		// synchronizing
		MONITOR_ENTER, MONITOR_EXIT,
		// miscellaneous
		ARRAY_LENGTH, A_THROW, LONG_COMPARE, NOP, POP, SWAP,
	
	}
	
	public static final class SimpleCommand extends JCommand {
		
		public final SimpleCommands commandType;
		
		public SimpleCommand(SimpleCommands commandType) {
			this.commandType = commandType;
		}
		
		@Override
		public String toString() {
			return this.commandType.toString();
		}
		
	}
	
}
