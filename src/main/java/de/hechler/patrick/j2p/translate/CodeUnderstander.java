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
import de.hechler.patrick.j2p.parse.JExceptionHandler;
import de.hechler.patrick.j2p.parse.JMethod;
import de.hechler.patrick.j2p.parse.JType;
import de.hechler.patrick.j2p.translate.AbstractCommand.MethodInvokation;

@SuppressWarnings("javadoc")
public class CodeUnderstander {
	
	private static final String CHECK_CAST = "check cast";
	
	enum ConstantZeroOrNull implements AbstractExpression.Constant {
		VALUE
	}
	
	record ConstantClass(JType cls) implements AbstractExpression.Constant {}
	
	record Constant(JVMType type, long value) implements AbstractExpression.Constant {}
	
	record AccessField(AbstractExpression reference, CPEntry.CPEFieldRef field) implements AbstractExpression.AccessableValue {}
	
	record AccessArray(JVMType atype, AbstractExpression reference, AbstractExpression index) implements AbstractExpression.AccessableValue {}
	
	record Parameter(int parameterIndex, JType parameterType) implements AbstractExpression.ParameterValue {}
	
	record InstanceOf(AbstractExpression a, JType cls) implements AbstractExpression.CalculationResult {}
	
	record Compare(AbstractExpression a, JVMCmp op, AbstractExpression b) implements AbstractExpression.CalculationResult {}
	
	record FPCompare(AbstractExpression a, AbstractExpression b, JVMType type, int nanValue) implements AbstractExpression.CalculationResult {}

	record Math(AbstractExpression a, JVMMath op, AbstractExpression b) implements AbstractExpression.CalculationResult {}

	record Convert(AbstractExpression a, JVMType from, JVMType to) implements AbstractExpression.CalculationResult {}
	
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
			cmdListIndex = understandCodeBlock(cmds, cmdListIndex, end, current.getValue(), jumps);
			current      = next;
		} while (current != null);
		throw new UnsupportedOperationException("thats too much for me");
	}
	
	private static Map<Integer, AbstractCodeBuilder> initJumps(JMethod method) {
		Map<Integer, AbstractCodeBuilder>      jumps = new TreeMap<>((a, b) -> Integer.compareUnsigned(a.intValue(), b.intValue()));
		Function<Integer, AbstractCodeBuilder> cia   = i -> new AbstractCodeBuilder(method);
		for (JCommand cmd : method.commands()) {
			if (cmd instanceof JCommand.Goto g) {
				jumps.computeIfAbsent(Integer.valueOf(g.targetAddress()), cia);
			}
		}
		if (method.handlers() != null) {
			for (JExceptionHandler cmd : method.handlers()) {
				jumps.computeIfAbsent(Integer.valueOf(cmd.handlerAddress()), cia);
			}
		}
		Integer             zero = Integer.valueOf(0);
		AbstractCodeBuilder acb  = jumps.computeIfAbsent(zero, cia);
		for (int i = 0; i < method.methodType.params().size(); i++) {
			acb.localVariables[i] = new Parameter(i, method.methodType.params().get(i));
		}
		return jumps;
	}
	
	private static int understandCodeBlock(List<JCommand> cmds, int cmdListIndex, int endAddress, AbstractCodeBuilder acb, Map<Integer, AbstractCodeBuilder> jumps) {
		long                     endAddressLong = endAddress & 0xFFFFFFFFL;
		List<AbstractCommand>    acbCmds        = acb.commands;
		List<AbstractExpression> acbOpStack     = acb.operantStack;
		AbstractExpression[]     acbLocVars     = acb.localVariables;
		List<AbstractExpression> acbLVL         = Arrays.asList(acbLocVars);
		JCommand                 cmd;
		do {
			cmd = cmds.get(cmdListIndex++);
			understandCommand(acbCmds, acbOpStack, acbLocVars, acbLVL, cmd, jumps,
					cmds.size() <= cmdListIndex ? endAddress : (int) cmds.get(cmdListIndex).address());
		} while (cmd.address() < endAddressLong);
		return cmdListIndex;
	}
	
	private static void understandCommand(List<AbstractCommand> commands, List<AbstractExpression> operantStack, AbstractExpression[] localVariables,
			List<AbstractExpression> lvl, JCommand um, Map<Integer, AbstractCodeBuilder> jumps, int commandEnd) throws AssertionError {
		if (um instanceof JCommand.PutField f) {
			AbstractExpression value     = operantStack.remove(operantStack.size() - 1);
			AbstractExpression objectref = f.instance ? operantStack.remove(operantStack.size() - 1) : null;
			commands.add(new AbstractCommand.Assign(new AccessField(objectref, f.field), value));
		} else if (um instanceof JCommand.GetField f) {
			AbstractExpression objectref = f.instance ? operantStack.remove(operantStack.size() - 1) : null;
			AccessField        access    = new AccessField(objectref, f.field);
			operantStack.add(access);
			commands.add(new AbstractCommand.Access(access));
		} else if (um instanceof JCommand.ArrayStore a) {
			AbstractExpression value     = operantStack.remove(operantStack.size() - 1);
			AbstractExpression index     = operantStack.remove(operantStack.size() - 1);
			AbstractExpression objectref = operantStack.remove(operantStack.size() - 1);
			commands.add(new AbstractCommand.Assign(new AccessArray(a.type, objectref, index), value));
		} else if (um instanceof JCommand.ArrayLoad a) {
			AbstractExpression index     = operantStack.remove(operantStack.size() - 1);
			AbstractExpression objectref = operantStack.remove(operantStack.size() - 1);
			AccessArray        access    = new AccessArray(a.type, objectref, index);
			operantStack.add(access);
			commands.add(new AbstractCommand.Access(access));
		} else if (um instanceof JCommand.CheckCast cc) {
			if (cc.fail) {
				AbstractExpression       objectref = operantStack.get(operantStack.size() - 1);
				List<AbstractExpression> args      = List.of(objectref, new ConstantClass(cc.type));
				MethodInvokation<?>      cmd       = new AbstractCommand.MethodInvokation<>(CHECK_CAST, args);
				commands.add(cmd);
			} else {
				AbstractExpression objectref = operantStack.remove(operantStack.size() - 1);
				operantStack.add(new InstanceOf(objectref, cc.type));
			}
		} else if (um instanceof JCommand.SignCheck sc) {
			AbstractExpression       a      = operantStack.remove(operantStack.size() - 1);
			AbstractExpression       b      = sc instanceof JCommand.Compare ? operantStack.remove(operantStack.size() - 1) : ConstantZeroOrNull.VALUE;
			List<AbstractExpression> params = new ArrayList<>();
			params.addAll(operantStack);
			params.addAll(lvl);
			AbstractExpression         condition = new Compare(a, sc.cmp, b);
			AbstractCommand.GotoTarget it        = new AbstractCommand.GotoTarget(jumps.get(Integer.valueOf(sc.targetAddress())));
			AbstractCommand.GotoTarget et        = new AbstractCommand.GotoTarget(jumps.get(Integer.valueOf(commandEnd)));
			commands.add(new AbstractCommand.IfGoto(condition, it, et, params));
		} else if (um instanceof JCommand.Goto g) {
			List<AbstractExpression> params = new ArrayList<>();
			params.addAll(operantStack);
			params.addAll(lvl);
			AbstractCommand.GotoTarget gt = new AbstractCommand.GotoTarget(jumps.get(Integer.valueOf(g.targetAddress())));
			commands.add(new AbstractCommand.Goto(gt, params));
		} else if (um instanceof JCommand.Const c) {
			operantStack.add(new Constant(c.type, c.value));
		} else if (um instanceof JCommand.Convert c) {
			AbstractExpression val = operantStack.remove(operantStack.size() - 1);
			operantStack.add(new Convert(val, c.from, c.to));
		} else if (um instanceof JCommand.FPCompare fpc) {
			AbstractExpression a = operantStack.remove(operantStack.size() - 1);
			AbstractExpression b = operantStack.remove(operantStack.size() - 1);
			operantStack.add(new FPCompare(a, b, fpc.type, fpc.nanValue));
		} else if (um instanceof JCommand.IInc i) {
			AbstractExpression oldVal = localVariables[i.localVarIndex];
			Constant add = new Constant(JVMType.INT, i.addConst);
			localVariables[i.localVarIndex] = new Math(oldVal, JVMMath.ADD, add);
		} else if (um instanceof JCommand.InvokeDynamic) {
			
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
			throw new AssertionError("unknown ActField: " + um);
		}
	}
	
}
