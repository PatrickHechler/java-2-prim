package de.hechler.patrick.j2p.translate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;

import de.hechler.patrick.j2p.parse.CPEntry;
import de.hechler.patrick.j2p.parse.ClassReader.JVMCmp;
import de.hechler.patrick.j2p.parse.ClassReader.JVMMath;
import de.hechler.patrick.j2p.parse.ClassReader.JVMType;
import de.hechler.patrick.j2p.parse.JCommand;
import de.hechler.patrick.j2p.parse.JCommand.Return;
import de.hechler.patrick.j2p.parse.JExceptionHandler;
import de.hechler.patrick.j2p.parse.JMethod;
import de.hechler.patrick.j2p.parse.JSMEVerificationInfo;
import de.hechler.patrick.j2p.parse.JStackMapEntry;
import de.hechler.patrick.j2p.parse.JStackMapEntry.FullDescribtion;
import de.hechler.patrick.j2p.parse.JType;

@SuppressWarnings("javadoc")
public class CodeUnderstander {
	
	private static final String CHECK_CAST = "check cast";
	
	enum ConstantZeroOrNull implements AbstractExpression.Constant {
		VALUE
	}
	
	public static record ConstantClass(JType cls) implements AbstractExpression.Constant {}
	
	public static record Constant(JVMType type, long value) implements AbstractExpression.Constant {}
	
	public static record AccessField(AbstractExpression reference, CPEntry.CPEFieldRef field) implements AbstractExpression.AccessableValue {}
	
	public static record AccessArray(JVMType atype, AbstractExpression reference, AbstractExpression index) implements AbstractExpression.AccessableValue {}
	
	public static record Parameter(int parameterIndex, JType parameterType) implements AbstractExpression.ParameterValue {}
	
	public static record InstanceOf(AbstractExpression a, JType cls) implements AbstractExpression.CalculationResult {}
	
	public static record Compare(AbstractExpression a, JVMCmp op, AbstractExpression b) implements AbstractExpression.CalculationResult {}
	
	public static record FPCompare(AbstractExpression a, AbstractExpression b, JVMType type, int nanValue) implements AbstractExpression.CalculationResult {}
	
	public static record MathCalc(AbstractExpression a, JVMMath op, AbstractExpression b) implements AbstractExpression.CalculationResult {}
	
	public static record Convert(AbstractExpression a, JVMType from, JVMType to) implements AbstractExpression.CalculationResult {}
	
	public AbstractCodeBuilder understand(JMethod method) {
		Map<Integer, AbstractCodeBuilder>             jumps        = initJumps(method);
		Iterator<Entry<Integer, AbstractCodeBuilder>> iter         = jumps.entrySet().iterator();
		Entry<Integer, AbstractCodeBuilder>           current      = iter.next();
		int                                           cmdListIndex = 0;
		List<JCommand>                                cmds         = method.commands();
		do {
			Entry<Integer, AbstractCodeBuilder> next  = iter.hasNext() ? iter.next() : null;
			int                                 start = current.getKey().intValue();
			int                                 end   = next == null ? method.codeLength() : next.getKey().intValue();
			while (cmds.get(cmdListIndex).address() != start) {
				if (cmds.get(cmdListIndex).address() > start) {
					throw new AssertionError();
				}
				System.err.println("[WARN]: unreachable command: " + method.file.thisClass() + " ==> " + method.name + " " + method.methodType + " at 0x"
						+ Long.toHexString(cmds.get(cmdListIndex).address()) + " : " + cmds.get(cmdListIndex).address() + " ==> " + cmds.get(cmdListIndex));
				cmdListIndex++;
			}
			cmdListIndex = understandCodeBlock(method, cmds, cmdListIndex, end, current.getValue(), jumps);
			current      = next;
		} while (current != null);
		throw new UnsupportedOperationException("thats too much for me");
	}
	
	private static Map<Integer, AbstractCodeBuilder> initJumps(JMethod method) {
		Map<Integer, AbstractCodeBuilder>      jumps = new TreeMap<>((a, b) -> Integer.compareUnsigned(a.intValue(), b.intValue()));
		Function<Integer, AbstractCodeBuilder> cia   = i -> new AbstractCodeBuilder(method);
		boolean una=false;
		for (JCommand cmd : method.commands()) {
			if (una) {
				una = false;
				jumps.computeIfAbsent(Integer.valueOf((int) cmd.address()), cia);
			}
			if (cmd instanceof JCommand.Goto g) {
				jumps.computeIfAbsent(Integer.valueOf(g.targetAddress()), cia);
				una = true;
			} else if (cmd instanceof Return) una = true;
		}
		if (method.handlers() != null) {
			for (JExceptionHandler cmd : method.handlers()) {
				jumps.computeIfAbsent(Integer.valueOf(cmd.handlerAddress()), cia);
			}
		}
		Integer             zero = Integer.valueOf(0);
		AbstractCodeBuilder acb  = jumps.computeIfAbsent(zero, cia);// the first stack entry is always a FullDescribtion
		JStackMapEntry[]    smes = method.stackMapEntries();
		acb.initParameters((FullDescribtion) smes[0], method); // do the first entry
		List<JSMEVerificationInfo> stack      = new ArrayList<>(method.maxStack());
		JSMEVerificationInfo[]     locals     = ((FullDescribtion) smes[0]).locals().clone();
		int                        address    = 0;
		int                        localCount = method.methodType.params().size();
		for (int i = 1; i < smes.length; i++) {
			JStackMapEntry sme = smes[i];
			int            off = sme.offsetDelta();
			address += off + 1;
			if (sme instanceof JStackMapEntry.FullDescribtion fd) {
				JSMEVerificationInfo[] fdl = fd.locals();
				System.arraycopy(fd.locals(), 0, locals, 0, fdl.length);
				Arrays.fill(locals, fdl.length, locals.length, null);
				JSMEVerificationInfo[] fds = fd.stack();
				stack.clear();
				stack.addAll(Arrays.asList(fds));
			} else if (sme instanceof JStackMapEntry.AppendedLocalsEmptyStack al) {
				stack.clear();
				JSMEVerificationInfo[] add = al.addedLocals();
				System.arraycopy(add, 0, locals, localCount, add.length);
				localCount += add.length;
			} else if (sme instanceof JStackMapEntry.SameLocalsNewStack ns) {
				stack.clear();
				stack.addAll(Arrays.asList(ns.stack()));
				int rl = ns.removedLocals();
				if (rl != 0) { // expect only a few chop locals
					Arrays.fill(locals, localCount - rl, localCount, null);
					localCount -= rl;
				}
			} else {
				throw new AssertionError("unknown JStackMapEntry type: " + sme.getClass());
			}
			acb = jumps.get(Integer.valueOf(address));
			if (acb != null) {
				acb.initParameters(locals, stack, method);
			}
		}
		return jumps;
	}
	
	private static int understandCodeBlock(JMethod method, List<JCommand> cmds, int cmdListIndex, int endAddress, AbstractCodeBuilder acb,
			Map<Integer, AbstractCodeBuilder> jumps) {
		long                     endAddressLong = endAddress & 0xFFFFFFFFL;
		List<AbstractCommand>    acbCmds        = acb.commands;
		List<AbstractExpression> acbOpStack     = acb.operantStack;
		AbstractExpression[]     acbLocVars     = acb.localVariables;
		List<AbstractExpression> acbLVL         = Arrays.asList(acbLocVars);
		JCommand                 cmd;
		do {
			cmd = cmds.get(cmdListIndex++);
			AbstractCommand addCmd = understandCommand(acbOpStack, acbLocVars, acbLVL, cmd, jumps,
					cmds.size() <= cmdListIndex ? endAddress : (int) cmds.get(cmdListIndex).address());
			if (addCmd != null) {
				acbCmds.add(addCmd);
				if (addCmd instanceof AbstractCommand.Goto || addCmd instanceof AbstractCommand.Return) {
					return cmdListIndex;
				}
				if (addCmd instanceof AbstractCommand.IfGoto ig) {
					if (!ig.elseTarget().nonFinalTarget().initilized() || !ig.ifTarget().nonFinalTarget().initilized()) {
						FullDescribtion desc = generateFullDesc(method, acb);
						if (!ig.elseTarget().nonFinalTarget().initilized()) {
							ig.elseTarget().nonFinalTarget().initParameters(desc, method);
						}
						if (!ig.ifTarget().nonFinalTarget().initilized()) {
							ig.ifTarget().nonFinalTarget().initParameters(desc, method);
						}
					}
					return cmdListIndex;
				}
			}
		} while (cmd.address() < endAddressLong);
		System.err.println("block ends on a non goto and non return command (type: " + acbCmds.get(acbCmds.size() - 1) + ")");
		return cmdListIndex;
	}
	
	private static FullDescribtion generateFullDesc(JMethod method, AbstractCodeBuilder from) {
		JSMEVerificationInfo[]     locs;
		List<JSMEVerificationInfo> stack = new ArrayList<>(Math.min(method.maxStack(), from.operantStack.size() << 1));
		int                        len;
		for (len = from.localVariables.length; from.localVariables[len - 1] == null; len--);
		if (bigJavaType(from.localVariables[len - 1])) len++;
		
		// TODO finish method
	}
	
	private static boolean bigJavaType(AbstractExpression ae) {
		if (ae instanceof Constant c) return c.type == JVMType.LONG || c.type == JVMType.DOUBLE;
		if (ae instanceof AccessField f) return f.field.cls() == JType.JPrimType.LONG || f.field.cls() == JType.JPrimType.DOUBLE;
		if (ae instanceof AccessArray f) return f.atype == JVMType.LONG || f.atype == JVMType.DOUBLE;
		if (ae instanceof Parameter p) return p.parameterType == JType.JPrimType.LONG || p.parameterType == JType.JPrimType.DOUBLE;
		if (ae instanceof MathCalc m) return bigJavaType(m.a);
		if (ae instanceof Convert c) return c.to == JVMType.LONG || c.to == JVMType.DOUBLE;
		if (ae instanceof FPCompare) return false;
		if (ae instanceof InstanceOf) return false;
		if (ae instanceof ConstantClass) return false;
		throw new AssertionError("unknown expression type: " + ae.getClass());
	}
	
	private static AbstractCommand understandCommand(List<AbstractExpression> operantStack, AbstractExpression[] localVariables, List<AbstractExpression> lvl,
			JCommand um, Map<Integer, AbstractCodeBuilder> jumps, int commandEnd) throws AssertionError {
		if (um instanceof JCommand.PutField f) {
			AbstractExpression value     = operantStack.remove(operantStack.size() - 1);
			AbstractExpression objectref = f.instance ? operantStack.remove(operantStack.size() - 1) : null;
			return new AbstractCommand.Assign(new AccessField(objectref, f.field), value);
		} else if (um instanceof JCommand.GetField f) {
			AbstractExpression objectref = f.instance ? operantStack.remove(operantStack.size() - 1) : null;
			AccessField        access    = new AccessField(objectref, f.field);
			operantStack.add(access);
			return new AbstractCommand.Access(access);
		} else if (um instanceof JCommand.ArrayStore a) {
			AbstractExpression value     = operantStack.remove(operantStack.size() - 1);
			AbstractExpression index     = operantStack.remove(operantStack.size() - 1);
			AbstractExpression objectref = operantStack.remove(operantStack.size() - 1);
			return new AbstractCommand.Assign(new AccessArray(a.type, objectref, index), value);
		} else if (um instanceof JCommand.ArrayLoad a) {
			AbstractExpression index     = operantStack.remove(operantStack.size() - 1);
			AbstractExpression objectref = operantStack.remove(operantStack.size() - 1);
			AccessArray        access    = new AccessArray(a.type, objectref, index);
			operantStack.add(access);
			return new AbstractCommand.Access(access);
		} else if (um instanceof JCommand.CheckCast cc) {
			if (cc.fail) {
				AbstractExpression       objectref = operantStack.get(operantStack.size() - 1);
				List<AbstractExpression> args      = List.of(objectref, new ConstantClass(cc.type));
				return new AbstractCommand.MethodInvokation<>(CHECK_CAST, args);
			}
			AbstractExpression objectref = operantStack.remove(operantStack.size() - 1);
			operantStack.add(new InstanceOf(objectref, cc.type));
			return null;
		} else if (um instanceof JCommand.SignCheck sc) {
			AbstractExpression       a      = operantStack.remove(operantStack.size() - 1);
			AbstractExpression       b      = sc instanceof JCommand.Compare ? operantStack.remove(operantStack.size() - 1) : ConstantZeroOrNull.VALUE;
			List<AbstractExpression> params = new ArrayList<>();
			params.addAll(operantStack);
			params.addAll(lvl);
			AbstractExpression         condition = new Compare(a, sc.cmp, b);
			AbstractCommand.GotoTarget it        = new AbstractCommand.GotoTarget(jumps.get(Integer.valueOf(sc.targetAddress())));
			AbstractCommand.GotoTarget et        = new AbstractCommand.GotoTarget(jumps.get(Integer.valueOf(commandEnd)));
			return new AbstractCommand.IfGoto(condition, it, et, params);
		} else if (um instanceof JCommand.Goto g) {
			List<AbstractExpression> params = new ArrayList<>();
			params.addAll(operantStack);
			params.addAll(lvl);
			AbstractCommand.GotoTarget gt = new AbstractCommand.GotoTarget(jumps.get(Integer.valueOf(g.targetAddress())));
			return new AbstractCommand.Goto(gt, params);
		} else if (um instanceof JCommand.Const c) {
			operantStack.add(new Constant(c.type, c.value));
			return null;
		} else if (um instanceof JCommand.Convert c) {
			AbstractExpression val = operantStack.remove(operantStack.size() - 1);
			operantStack.add(new Convert(val, c.from, c.to));
			return null;
		} else if (um instanceof JCommand.FPCompare fpc) {
			AbstractExpression a = operantStack.remove(operantStack.size() - 1);
			AbstractExpression b = operantStack.remove(operantStack.size() - 1);
			operantStack.add(new FPCompare(a, b, fpc.type, fpc.nanValue));
			return null;
		} else if (um instanceof JCommand.IInc i) {
			AbstractExpression oldVal = localVariables[i.localVarIndex];
			Constant           add    = new Constant(JVMType.INT, i.addConst);
			localVariables[i.localVarIndex] = new MathCalc(oldVal, JVMMath.ADD, add);
			return null;
		} else if (um instanceof JCommand.InvokeDynamic id) {
			
		} else if (um instanceof JCommand.InvokeInterface) {
			
		} else if (um instanceof JCommand.InvokeNormal) {
			
		} else if (um instanceof JCommand.LoadConstPool) {
			
		} else if (um instanceof JCommand.LocalVariableLoad) {
			
		} else if (um instanceof JCommand.LocalVariableStore) {
			
		} else if (um instanceof JCommand.InvokeVirtual) {
			
		} else if (um instanceof JCommand.InvokeSpecial) {
			
		} else if (um instanceof JCommand.LookupSwitch) {
			
		} else if (um instanceof JCommand.InvokeStatic) {
			
		} else if (um instanceof JCommand.MultiNewArray) {
			
		} else if (um instanceof JCommand.New) {
			
		} else if (um instanceof JCommand.NewArray) {
			
		} else if (um instanceof JCommand.PrimMath) {
			
		} else if (um instanceof JCommand.Push) {
			
		} else if (um instanceof JCommand.Return) {
			
		} else if (um instanceof JCommand.SimpleCommand) {
			
		} else if (um instanceof JCommand.SignCheck) {
			
		} else if (um instanceof JCommand.StackDup) {
			
		} else if (um instanceof JCommand.TableSwitch) {
			
		} else {
			throw new AssertionError("unknown Command: " + um);
		}
		throw new UnsupportedOperationException("this command is not yet done!");
	}
	
}
