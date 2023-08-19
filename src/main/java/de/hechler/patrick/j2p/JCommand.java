package de.hechler.patrick.j2p;

import de.hechler.patrick.j2p.ClassReader.JVMCmp;
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
		// miscellaneous
		ARRAY_LENGTH, A_THROW, LONG_COMPARE, NOP
	
	}
	
	public static final class SimpleCommand extends JCommand {
		
		public final SimpleCommands commandType;
		
		public SimpleCommand(SimpleCommands commandType) {
			this.commandType = commandType;
		}
		
	}
	
}
