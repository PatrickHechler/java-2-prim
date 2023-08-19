package de.hechler.patrick.j2p;

import de.hechler.patrick.j2p.ClassReader.JVMCmp;
import de.hechler.patrick.j2p.ClassReader.JVMMath;
import de.hechler.patrick.j2p.ClassReader.JVMType;

@SuppressWarnings("javadoc")
public abstract sealed class JCommand {
	
	private long addr = -1L;
	
	public void initAdress(long addr) {
		if (this.addr != -1L) {
			throw new AssertionError();
		}
		this.addr = addr;
	}
	
	public static final class NewArray extends JCommand {
		
		public final JType componentType;
		
		public NewArray(JType componentType) {
			this.componentType = componentType;
		}
		
	}
	
	public static sealed class InvokeNormal extends JCommand {
		
		public final CPEntry.CPENormalMethodRef invoke;
		
		public InvokeNormal(CPEntry.CPENormalMethodRef invoke) {
			this.invoke = invoke;
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
		
	}
	
	public static final class FPCompare extends JCommand {
		
		public final JVMType type;
		public final int     nanValue;
		
		public FPCompare(JVMType type, int nanValue) {
			this.type     = type;
			this.nanValue = nanValue;
		}
		
	}
	
	public static final class StackDup extends JCommand {
		
		public final int skip;
		public final int dupCnt;
		
		public StackDup(int skip, int dupCnt) {
			this.skip   = skip;
			this.dupCnt = dupCnt;
		}
		
	}
	
	public static final class IInc extends JCommand {
		
		public final int localVarIndex;
		public final int addConst;
		
		public IInc(int localVarIndex, int addConst) {
			this.localVarIndex = localVarIndex;
			this.addConst      = addConst;
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
		
	}
	
	public static final class Convert extends JCommand {
		
		public final JVMType from;
		public final JVMType to;
		
		public Convert(JVMType from, JVMType to) {
			this.from = from;
			this.to   = to;
		}
		
	}
	
	public static final class CheckCast extends JCommand {
		
		public final JType   type;
		public final boolean fail;
		
		public CheckCast(JType type, boolean fail) {
			this.type = type;
			this.fail = fail;
		}
		
	}
	
	public static final class Const extends JCommand {
		
		public final JVMType type;
		public final long    value;
		
		public Const(JVMType type, long value) {
			this.type  = type;
			this.value = value;
		}
		
	}
	
	public static final class LocalLoad extends JCommand {
		
		public final JVMType type;
		public final int     index;
		
		public LocalLoad(JVMType type, int index) {
			this.type  = type;
			this.index = index;
		}
		
	}
	
	public static final class LocalStore extends JCommand {
		
		public final JVMType type;
		public final int     index;
		
		public LocalStore(JVMType type, int index) {
			this.type  = type;
			this.index = index;
		}
		
	}
	
	public static final class PrimMath extends JCommand {
		
		public final JVMType type;
		public final JVMMath op;
		
		public PrimMath(JVMType type, JVMMath op) {
			this.type = type;
			this.op   = op;
		}
		
	}
	
	public static final class MultiNewArray extends JCommand {
		
		public final JType type;
		public final int   dimensions;
		
		public MultiNewArray(JType type, int dimensions) {
			this.type       = type;
			this.dimensions = dimensions;
		}
		
	}
	
	public static final class Push extends JCommand {
		
		public final int bytes;
		public final int value;
		
		public Push(int bytes, int value) {
			this.bytes = bytes;
			this.value = value;
		}
		
	}
	
	public static final class New extends JCommand {
		
		public final JType type;
		
		public New(JType type) {
			this.type = type;
		}
		
	}
	
	public static final class Return extends JCommand {
		
		public final JVMType type;
		
		public Return(JVMType type) {
			this.type = type;
		}
		
	}
	
	public static final class ArrayLoad extends JCommand {
		
		public final JVMType type;
		
		public ArrayLoad(JVMType type) {
			this.type = type;
		}
		
	}
	
	public static final class ArrayStore extends JCommand {
		
		public final JVMType type;
		
		public ArrayStore(JVMType type) {
			this.type = type;
		}
		
	}
	
	public static final class LoadConstPool extends JCommand {
		
		public final CPEntry entry;
		
		public LoadConstPool(CPEntry entry) {
			this.entry = entry;
		}
		
	}
	
	public static sealed class Goto extends JCommand {
		
		public final int relativeAdress;
		
		public Goto(int relativeAdress) {
			this.relativeAdress = relativeAdress;
		}
		
	}
	
	public static sealed class Sign extends Goto {
		
		public final JVMType type;
		public final JVMCmp  cmp;
		
		public Sign(int relativeAdress, JVMType type, JVMCmp cmp) {
			super(relativeAdress);
			this.type = type;
			this.cmp  = cmp;
		}
		
	}
	
	public static final class Compare extends Sign {
		
		public Compare(int relativeAdress, JVMType type, JVMCmp cmp) {
			super(relativeAdress, type, cmp);
		}
		
	}
	
	public enum SimpleCommands {
		// get/set fields
		GET_FIELD, GET_STATIC_FIELD, SET_FIELD, SET_STATIC_FIELD,
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
		
	}
	
}
