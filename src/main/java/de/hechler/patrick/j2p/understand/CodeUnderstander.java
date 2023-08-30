package de.hechler.patrick.j2p.understand;

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
import de.hechler.patrick.j2p.parse.JType;
import de.hechler.patrick.j2p.parse.MethodType;

@SuppressWarnings("javadoc")
public class CodeUnderstander {
	
	public static record AgeExp(AbstractExpression exp, int birth) {}
	
	public static record AgeCmd(AbstractCommand exp, int birth) {}
	
	public static record ThrowExpression(AgeExp reference) implements AbstractExpression {}
	
	public static record NewObject(JType type) implements AbstractExpression.CalculationResult {}
	
	public static record NewArray(JType.ArrayType arraytype, AgeExp[] dimensions) implements AbstractExpression.CalculationResult {
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.arraytype.hashCode();
			result = prime * result + Arrays.hashCode(this.dimensions);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof NewArray)) return false;
			NewArray other = (NewArray) obj;
			if (!this.arraytype.equals(other.arraytype)) return false;
			return Arrays.equals(this.dimensions, other.dimensions);
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("NewArray [arraytype=");
			builder.append(this.arraytype);
			builder.append(", dimensions=");
			builder.append(Arrays.toString(this.dimensions));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public static record MethodResult(JType type, JCommand invokeCmd) implements AbstractExpression.CalculationResult {}
	
	public static record ConstantDynamic(JType type, CPEntry.CPEDynamic dynamic) implements AbstractExpression.Constant {}
	
	public static record ConstantMethodType(MethodType type) implements AbstractExpression.Constant {}
	
	public static record ConstantMethodHandle(CPEntry.CPEMethodHandle handle) implements AbstractExpression.Constant {}
	
	public static record ConstantClass(JType value) implements AbstractExpression.Constant {}
	
	public static record ConstantString(String value) implements AbstractExpression.Constant {}
	
	public static record Constant(JVMType type, long value) implements AbstractExpression.Constant {}
	
	public static record AccessField(AgeExp reference, CPEntry.CPEFieldRef field) implements AbstractExpression.AccessableValue {}
	
	public static record AccessArray(JVMType atype, AgeExp reference, AgeExp index) implements AbstractExpression.AccessableValue {}
	
	public static record Parameter(int parameterIndex, JType parameterType) implements AbstractExpression.ParameterValue {}
	
	public static record InstanceOf(AgeExp a, JType cls) implements AbstractExpression.CalculationResult {}
	
	public static record Compare(AgeExp a, JVMCmp op, AgeExp b) implements AbstractExpression.CalculationResult {}
	
	public static record FPCompare(AgeExp a, AgeExp b, JVMType type, int nanValue) implements AbstractExpression.CalculationResult {}
	
	public static record LongCompare(AgeExp a, AgeExp b) implements AbstractExpression.CalculationResult {}
	
	public static record ArrayLength(AgeExp array) implements AbstractExpression.CalculationResult {}
	
	public static record MathCalcBinary(AgeExp a, JVMMath op, AgeExp b) implements AbstractExpression.CalculationResult {}
	
	public static record MathCalcUnary(JVMMath op, AgeExp a) implements AbstractExpression.CalculationResult {}
	
	public static record Convert(AgeExp a, JVMType from, JVMType to) implements AbstractExpression.CalculationResult {}
	
	public Map<Integer, AbstractCodeBuilder> understand(JMethod method) {
		Map<Integer, AbstractCodeBuilder> jumps = initJumps(method);
		System.out.println("  code blocks: " + jumps);
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
				System.err.println("[WARN]: unreachable command: " + method.file.thisClass() + " ==> " + method.name + method.methodType + " at 0x"
						+ Long.toHexString(cmds.get(cmdListIndex).address()) + " : " + cmds.get(cmdListIndex).address() + " ==> " + cmds.get(cmdListIndex));
				cmdListIndex++;
			}
			cmdListIndex = understandCodeBlock(method, cmds, cmdListIndex, end, current.getValue(), jumps);
			current      = next;
		} while (current != null);
		return jumps;
	}
	
	private static Map<Integer, AbstractCodeBuilder> initJumps(JMethod method) {
		Map<Integer, AbstractCodeBuilder>      jumps = new TreeMap<>((a, b) -> Integer.compareUnsigned(a.intValue(), b.intValue()));
		Function<Integer, AbstractCodeBuilder> cia   = i -> new AbstractCodeBuilder(method);
		boolean                                una   = false;
		for (JCommand cmd : method.commands()) {
			if (una) {
				una = false;
				jumps.computeIfAbsent(Integer.valueOf((int) cmd.address()), cia);
			}
			if (cmd instanceof JCommand.Goto g) {
				jumps.computeIfAbsent(Integer.valueOf(g.targetAddress()), cia);
				una = true;
			} else if (cmd instanceof JCommand.LookupSwitch || cmd instanceof JCommand.TableSwitch) {
				int   addr   = (int) cmd.address();
				int   defOff = cmd instanceof JCommand.LookupSwitch l ? l.defaultOffset : ((JCommand.TableSwitch) cmd).defaultOffset;
				int[] offs   = cmd instanceof JCommand.LookupSwitch l ? l.offsets : ((JCommand.TableSwitch) cmd).offsets;
				jumps.computeIfAbsent(Integer.valueOf(defOff), cia);
				for (int off : offs) {
					jumps.computeIfAbsent(Integer.valueOf(off + addr), cia);
				}
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
		acb.initParameters((JStackMapEntry.FullDescribtion) smes[0], method); // do the first entry
		List<JSMEVerificationInfo> stack      = new ArrayList<>(method.maxStack());
		JSMEVerificationInfo[]     locals     = Arrays.copyOf(((JStackMapEntry.FullDescribtion) smes[0]).locals().clone(), method.maxLocals());
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
		for (Entry<Integer, AbstractCodeBuilder> e : jumps.entrySet()) {
			e.getValue().setName("Code[at " + e.getKey().intValue() + " : 0x" + Integer.toHexString(e.getKey().intValue()).toUpperCase() + " ]");
		}
		return jumps;
	}
	
	private static int understandCodeBlock(JMethod method, List<JCommand> cmds, int cmdListIndex, int endAddress, AbstractCodeBuilder acb,
			Map<Integer, AbstractCodeBuilder> jumps) {
		long         endAddressLong = endAddress & 0xFFFFFFFFL;
		int          cmdsSize       = cmds.size();
		List<AgeCmd> acbCmds        = acb.commands;
		List<AgeExp> acbOpStack     = acb.operantStack;
		AgeExp[]     acbLocVars     = acb.localVariables;
		List<AgeExp> acbLVL         = Arrays.asList(acbLocVars);
		JCommand     cmd            = cmds.get(cmdListIndex);
		int          birth          = 1;
		do {
			JCommand next   = cmdListIndex + 1 < cmdsSize ? cmds.get(cmdListIndex + 1) : null;
			AgeCmd   addCmd = understandCommand(method, acbOpStack, acbLocVars, acbLVL, cmd, jumps, next != null ? (int) next.address() : endAddress, birth);
			if (addCmd != null) {
				acbCmds.add(addCmd);
				if (addCmd.exp() instanceof AbstractCommand.IfGoto ig) {
					if (!ig.elseTarget().nonFinalTarget().initilized() || !ig.ifTarget().nonFinalTarget().initilized()) {
						JStackMapEntry.FullDescribtion desc = generateFullDesc(method, acb);
						if (!ig.elseTarget().nonFinalTarget().initilized()) {
							ig.elseTarget().nonFinalTarget().initParameters(desc, method);
						}
						if (!ig.ifTarget().nonFinalTarget().initilized()) {
							ig.ifTarget().nonFinalTarget().initParameters(desc, method);
						}
					}
					return cmdListIndex + 1;
				} else if (addCmd.exp() instanceof AbstractCommand.Goto || addCmd.exp() instanceof AbstractCommand.Return) {
					return cmdListIndex + 1;
				} else if (addCmd.exp() instanceof AbstractCommand.Switch<?> s) {
					Object mapper = s.mapper();
					int    def;
					int[]  offs;
					if (mapper instanceof JCommand.TableSwitch ts) {
						def  = ts.defaultOffset;
						offs = ts.offsets;
					} else if (mapper instanceof JCommand.LookupSwitch ls) {
						def  = ls.defaultOffset;
						offs = ls.offsets;
					} else {
						throw new AssertionError("unknown mapper type: " + mapper.getClass());
					}
					JStackMapEntry.FullDescribtion desc = null;
					acb = jumps.get(Integer.valueOf(def));
					if (!acb.initilized()) {
						desc = generateFullDesc(method, acb);
						acb.initParameters(desc, method);
					}
					for (int off : offs) {
						acb = jumps.get(Integer.valueOf(off));
						if (!acb.initilized()) {
							if (desc == null) desc = generateFullDesc(method, acb);
							acb.initParameters(desc, method);
						}
					}
				}
			}
			cmd = next;
			cmdListIndex++;
			birth++;
		} while (cmd.address() < endAddressLong);
		List<AgeExp>        params = generateGotoParams(acbOpStack, acbLVL);
		AbstractCodeBuilder target = jumps.get(Integer.valueOf((int) cmds.get(cmdListIndex).address()));
		if (target == null) {
			throw new AssertionError("missing address: " + cmds.get(cmdListIndex).address() + " : 0x" + Long.toHexString(cmds.get(cmdListIndex).address())
					+ " code blocks: " + jumps.values());
		}
		AbstractCommand.GotoTarget gt       = new AbstractCommand.GotoTarget(target);
		AbstractCommand            gotoNext = new AbstractCommand.Goto(gt, params);
		acbCmds.add(new AgeCmd(gotoNext, birth));
		return cmdListIndex;
	}
	
	private static JStackMapEntry.FullDescribtion generateFullDesc(JMethod method, AbstractCodeBuilder from) {
		JSMEVerificationInfo[]     locs;
		List<JSMEVerificationInfo> stack = new ArrayList<>(Math.min(method.maxStack(), from.operantStack.size() << 1));
		int                        len;
		for (len = from.localVariables.length; from.localVariables[len - 1] == null; len--) {/**/}
		if (bigType(from.localVariables[len - 1].exp)) len++;
		locs = new JSMEVerificationInfo[len];
		for (int li = 0, fi = 0; li < locs.length; li++, fi++) {
			JSMEVerificationInfo vt = verifyType(from.localVariables[fi].exp);
			locs[li] = vt;
			if (vt == JSMEVerificationInfo.SimpleInfo.LONG || vt == JSMEVerificationInfo.SimpleInfo.DOUBLE) {
				locs[li++] = JSMEVerificationInfo.SimpleInfo.TOP;
			}
		}
		for (AgeExp sae : from.operantStack) {
			JSMEVerificationInfo vt = verifyType(sae.exp);
			stack.add(vt);
			if (vt == JSMEVerificationInfo.SimpleInfo.LONG || vt == JSMEVerificationInfo.SimpleInfo.DOUBLE) {
				stack.add(JSMEVerificationInfo.SimpleInfo.TOP);
			}
		}
		return new JStackMapEntry.FullDescribtion(-2, locs, stack.toArray(new JSMEVerificationInfo[stack.size()]));
	}
	
	private static boolean bigType(AbstractExpression ae) {
		if (ae instanceof Constant c) return c.type == JVMType.LONG || c.type == JVMType.DOUBLE;
		if (ae instanceof AccessField f) return f.field.cls() == JType.JPrimType.LONG || f.field.cls() == JType.JPrimType.DOUBLE;
		if (ae instanceof AccessArray f) return f.atype == JVMType.LONG || f.atype == JVMType.DOUBLE;
		if (ae instanceof Parameter p) return p.parameterType == JType.JPrimType.LONG || p.parameterType == JType.JPrimType.DOUBLE;
		if (ae instanceof MathCalcBinary m) return bigType(m.a.exp);
		if (ae instanceof MathCalcUnary m) return bigType(m.a.exp);
		if (ae instanceof Convert c) return c.to == JVMType.LONG || c.to == JVMType.DOUBLE;
		if (ae instanceof FPCompare) return false;
		if (ae instanceof InstanceOf) return false;
		if (ae instanceof ArrayLength) return false;
		if (ae instanceof LongCompare) return false;
		if (ae instanceof FPCompare) return false;
		if (ae instanceof ConstantString) return false;
		if (ae instanceof ConstantClass) return false;
		if (ae instanceof ConstantMethodHandle) return false;
		if (ae instanceof ConstantMethodType) return false;
		if (ae instanceof ConstantDynamic d) return d.type == JType.JPrimType.LONG || d.type == JType.JPrimType.DOUBLE;
		if (ae instanceof MethodResult m) return m.type == JType.JPrimType.LONG || m.type == JType.JPrimType.DOUBLE;
		if (ae instanceof NewArray) return false;
		if (ae instanceof NewObject) return false;
		throw new AssertionError("unknown/invalid expression type: " + ae.getClass());
	}
	
	private static JSMEVerificationInfo verifyType(AbstractExpression ae) {
		if (ae instanceof Constant c) return jvmTypeToVerifyToy(c.type);
		if (ae instanceof AccessField f) return jtypeToVerifyType(f.field.cls());
		if (ae instanceof AccessArray f) return jvmTypeToVerifyToy(f.atype);
		if (ae instanceof Parameter p) return jtypeToVerifyType(p.parameterType);
		if (ae instanceof MathCalcBinary m) return verifyType(m.a.exp);
		if (ae instanceof MathCalcUnary m) return verifyType(m.a.exp);
		if (ae instanceof Convert c) return jvmTypeToVerifyToy(c.to);
		if (ae instanceof FPCompare c) return jvmTypeToVerifyToy(c.type);
		if (ae instanceof InstanceOf) return JSMEVerificationInfo.SimpleInfo.INTEGER;
		if (ae instanceof ArrayLength) return JSMEVerificationInfo.SimpleInfo.INTEGER;
		if (ae instanceof LongCompare) return JSMEVerificationInfo.SimpleInfo.INTEGER;
		if (ae instanceof FPCompare) return JSMEVerificationInfo.SimpleInfo.INTEGER;
		if (ae instanceof ConstantString) return JSMEVerificationInfo.STRING_TYPE;
		if (ae instanceof ConstantClass) return JSMEVerificationInfo.CLASS_TYPE;
		if (ae instanceof ConstantMethodHandle) return JSMEVerificationInfo.METHOD_HANDLE_TYPE;
		if (ae instanceof ConstantMethodType) return JSMEVerificationInfo.METHOD_TYPE_TYPE;
		if (ae instanceof ConstantDynamic d) return jtypeToVerifyType(d.type);
		if (ae instanceof MethodResult m) return jtypeToVerifyType(m.type);
		if (ae instanceof NewArray n) return new JSMEVerificationInfo.ObjectInfo(n.arraytype);
		if (ae instanceof NewObject n) return new JSMEVerificationInfo.ObjectInfo(n.type);
		throw new AssertionError("unknown/invalid expression type: " + ae.getClass());
	}
	
	private static JSMEVerificationInfo jvmTypeToVerifyToy(JVMType vmt) {
		switch (vmt) {
		case CHAR, BYTE_BOOL, SHORT, INT:
			return JSMEVerificationInfo.SimpleInfo.INTEGER;
		case LONG:
			return JSMEVerificationInfo.SimpleInfo.LONG;
		case DOUBLE:
			return JSMEVerificationInfo.SimpleInfo.DOUBLE;
		case FLOAT:
			return JSMEVerificationInfo.SimpleInfo.FLOAT;
		case REFERENCE:
			return JSMEVerificationInfo.OBJECT_TYPE;
		case VOID:
			throw new AssertionError("stack entry or local variable has JVMType VOID");
		default:
			throw new AssertionError("unknown JVMType: " + vmt.name());
		}
	}
	
	private static JSMEVerificationInfo jtypeToVerifyType(JType jt) {
		if (jt instanceof JType.JPrimType p) {
			switch (p) {
			case BOOLEAN, BYTE, CHAR, INT, SHORT:
				return JSMEVerificationInfo.SimpleInfo.INTEGER;
			case DOUBLE:
				return JSMEVerificationInfo.SimpleInfo.DOUBLE;
			case FLOAT:
				return JSMEVerificationInfo.SimpleInfo.FLOAT;
			case LONG:
				return JSMEVerificationInfo.SimpleInfo.LONG;
			case VOID:
				throw new AssertionError("stack entry or local variable has JVMType VOID");
			default:
				throw new AssertionError("unknown JType.JPrimType value: " + p.name());
			}
		} else if (jt instanceof JType.NullType) {
			return JSMEVerificationInfo.SimpleInfo.NULL;
		} else if (jt instanceof JType.ObjectType || jt instanceof JType.ArrayType) {
			return new JSMEVerificationInfo.ObjectInfo(jt);
		} else {
			throw new AssertionError("unknown JType type: " + jt.getClass());
		}
	}
	
	private static AgeCmd understandCommand(JMethod method, List<AgeExp> operantStack, AgeExp[] localVariables, List<AgeExp> lvl, JCommand um,
			Map<Integer, AbstractCodeBuilder> jumps, int commandEnd, int birth) throws AssertionError {
		if (um instanceof JCommand.PutField f) {
			AgeExp value     = operantStack.remove(operantStack.size() - 1);
			AgeExp objectref = f.instance ? operantStack.remove(operantStack.size() - 1) : null;
			return new AbstractCommand.Assign(new AccessField(objectref, f.field).age(birth), value).age(birth);
		} else if (um instanceof JCommand.GetField f) {
			AgeExp objectref = f.instance ? operantStack.remove(operantStack.size() - 1) : null;
			AgeExp access    = new AccessField(objectref, f.field).age(birth);
			operantStack.add(access);
			return new AbstractCommand.Access(access).age(birth);
		} else if (um instanceof JCommand.ArrayStore a) {
			AgeExp value     = operantStack.remove(operantStack.size() - 1);
			AgeExp index     = operantStack.remove(operantStack.size() - 1);
			AgeExp objectref = operantStack.remove(operantStack.size() - 1);
			return new AbstractCommand.Assign(new AccessArray(a.type, objectref, index).age(birth), value).age(birth);
		} else if (um instanceof JCommand.ArrayLoad a) {
			AgeExp index     = operantStack.remove(operantStack.size() - 1);
			AgeExp objectref = operantStack.remove(operantStack.size() - 1);
			AgeExp access    = new AccessArray(a.type, objectref, index).age(birth);
			operantStack.add(access);
			return new AbstractCommand.Access(access).age(birth);
		} else if (um instanceof JCommand.CheckCast cc) {
			if (cc.fail) {
				AgeExp objectref = operantStack.get(operantStack.size() - 1);
				return new AbstractCommand.Assert<>(cc.type, objectref).age(birth);
			}
			AgeExp objectref = operantStack.remove(operantStack.size() - 1);
			operantStack.add(new InstanceOf(objectref, cc.type).age(birth));
			return null;
		} else if (um instanceof JCommand.Const c) {
			operantStack.add(new Constant(c.type, c.value).age(birth));
			return null;
		} else if (um instanceof JCommand.Convert c) {
			AgeExp val = operantStack.remove(operantStack.size() - 1);
			operantStack.add(new Convert(val, c.from, c.to).age(birth));
			return null;
		} else if (um instanceof JCommand.FPCompare fpc) {
			AgeExp b = operantStack.remove(operantStack.size() - 1);
			AgeExp a = operantStack.remove(operantStack.size() - 1);
			operantStack.add(new FPCompare(a, b, fpc.type, fpc.nanValue).age(birth));
			return null;
		} else if (um instanceof JCommand.IInc i) {
			AgeExp oldVal = localVariables[i.localVarIndex];
			AgeExp add    = new Constant(JVMType.INT, i.addConst).age(birth);
			localVariables[i.localVarIndex] = new MathCalcBinary(oldVal, JVMMath.ADD, add).age(birth);
			return null;
		} else if (um instanceof JCommand.InvokeDynamic i) {
			return new AbstractCommand.MethodInvokation<>(i, methodCall(operantStack, i.invoke.methodType(), i, birth)).age(birth);
		} else if (um instanceof JCommand.InvokeNormal i) {
			return new AbstractCommand.MethodInvokation<JCommand.InvokeNormal>(i, methodCall(operantStack, i.invoke.type(), i, birth)).age(birth);
		} else if (um instanceof JCommand.LoadConstPool l) {
			CPEntry cpe = l.entry;
			if (cpe instanceof CPEntry.CPEInt e) {
				operantStack.add(new Constant(JVMType.INT, e.val()).age(birth));
			} else if (cpe instanceof CPEntry.CPEFloat e) {
				operantStack.add(new Constant(JVMType.FLOAT, Float.floatToRawIntBits(e.val())).age(birth));
			} else if (cpe instanceof CPEntry.CPELong e) {
				operantStack.add(new Constant(JVMType.LONG, e.val()).age(birth));
			} else if (cpe instanceof CPEntry.CPEDouble e) {
				operantStack.add(new Constant(JVMType.DOUBLE, Double.doubleToRawLongBits(e.val())).age(birth));
			} else if (cpe instanceof CPEntry.CPEClass e) {
				operantStack.add(new ConstantClass(e.type()).age(birth));
			} else if (cpe instanceof CPEntry.CPEString e) {
				operantStack.add(new ConstantString(e.str()).age(birth));
			} else if (cpe instanceof CPEntry.CPEMethodHandle e) {
				operantStack.add(new ConstantMethodHandle(e).age(birth));
			} else if (cpe instanceof CPEntry.CPEMethodType e) {
				operantStack.add(new ConstantMethodType(e.type()).age(birth));
			} else if (cpe instanceof CPEntry.CPEDynamic e) {
				operantStack.add(new ConstantDynamic(e.fieldType(), e).age(birth));
			} else {
				throw new ClassFormatError("the load instruction tries to load a non-laodable value");
			}
			return null;
		} else if (um instanceof JCommand.LocalVariableLoad l) {
			operantStack.add(localVariables[l.index]);
			return null;
		} else if (um instanceof JCommand.LocalVariableStore l) {
			AgeExp ae = operantStack.remove(operantStack.size() - 1);
			localVariables[l.index] = ae;
			if (l.type == JVMType.LONG || l.type == JVMType.DOUBLE) {
				localVariables[l.index + 1] = null; // needed if l.index is overwritten with a small value
			}
			return null;
		} else if (um instanceof JCommand.New n) {
			AgeExp ne = new NewObject(n.type).age(birth);
			operantStack.add(ne);
			return new AbstractCommand.New(ne).age(birth);
		} else if (um instanceof JCommand.NewArray n) {
			AgeExp[] sizes = new AgeExp[] { operantStack.remove(operantStack.size() - 1) };
			AgeExp   ne    = new NewArray(new JType.ArrayType(n.componentType), sizes).age(birth);
			operantStack.add(ne);
			return new AbstractCommand.New(ne).age(birth);
		} else if (um instanceof JCommand.MultiNewArray n) {
			AgeExp[] sizes = new AgeExp[n.dimensions];
			for (int si = sizes.length; --si >= 0;) { // do decrement loop because the innermost length is to top stack entry
				sizes[si] = operantStack.remove(operantStack.size() - 1);
			}
			AgeExp ne = new NewArray(n.type, sizes).age(birth);
			operantStack.add(ne);
			return new AbstractCommand.New(ne).age(birth);
		} else if (um instanceof JCommand.PrimMath m) {
			switch (m.op) {
			case ADD, SUB, MUL, DIV, MOD, AND, XOR, OR, SHIFT_LEFT, SHIFT_ARITMETIC_RIGTH, SHIFT_LOGIC_RIGTH -> {
				AgeExp b = operantStack.remove(operantStack.size() - 1);
				AgeExp a = operantStack.remove(operantStack.size() - 1);
				operantStack.add(new MathCalcBinary(a, m.op, b).age(birth));
			}
			case NEG, NOT -> {
				AgeExp a = operantStack.remove(operantStack.size() - 1);
				operantStack.add(new MathCalcUnary(m.op, a).age(birth));
			}
			default -> throw new AssertionError("unknown JVMMath value: " + m.op.name());
			}
			return null;
		} else if (um instanceof JCommand.Pop p) {
			for (int pop = p.pops; pop > 0;) {
				AgeExp ae = operantStack.remove(operantStack.size() - 1);
				if (bigType(ae.exp)) {
					pop -= 2;
					if (pop < 0) {
						throw new VerifyError("tries to pop a hald long/double value from the operant stack");
					}
				} else pop--;
			}
			return null;
		} else if (um instanceof JCommand.Push p) {
			operantStack.add(new Constant(p.type, p.value).age(birth));
			return null;
		} else if (um instanceof JCommand.StackDup d) {
			AgeExp dup  = operantStack.get(operantStack.size() - 1);
			AgeExp dup0 = d.dupCnt == 2 && !bigType(dup.exp) ? operantStack.get(operantStack.size() - 2) : null;
			if (d.dupCnt < 1 || d.dupCnt > 2) {
				throw new AssertionError();
			}
			int insert = operantStack.size();
			for (int skipCnt = d.skip; skipCnt > 0; insert--) {
				if (bigType(operantStack.get(insert).exp)) {
					skipCnt -= 2;
					if (skipCnt < 0) {
						throw new VerifyError("dup* used for with an invalid insertion point (the middle of an long/double value)");
					}
				} else skipCnt--;
			}
			operantStack.add(insert, dup);
			if (dup0 != null) operantStack.add(insert, dup0);
			return null;
		} else if (um instanceof JCommand.SimpleCommand c) {
			switch (c.commandType) {
			case A_THROW: {
				AgeExp objectref = operantStack.get(operantStack.size() - 1);
				return new AbstractCommand.Return(new ThrowExpression(objectref).age(birth)).age(birth);
			}
			case ARRAY_LENGTH: {
				AgeExp arr = operantStack.remove(operantStack.size() - 1);
				operantStack.add(new ArrayLength(arr).age(birth));
				return null;
			}
			case LONG_COMPARE:
				AgeExp b = operantStack.remove(operantStack.size() - 1);
				AgeExp a = operantStack.remove(operantStack.size() - 1);
				operantStack.add(new LongCompare(a, b).age(birth));
				return null;
			case MONITOR_ENTER, MONITOR_EXIT:
				// TODO implement when synchronization/threading is supported
				return null;
			case NOP:
				return null;
			case SWAP:
				operantStack.set(operantStack.size() - 1, operantStack.set(operantStack.size() - 2, operantStack.get(operantStack.size() - 1)));
				return null;
			default:
				throw new AssertionError("unknown JCommand.SimpleCommand.commandType: " + c.commandType.name());
			}
		} else if (um instanceof JCommand.Return r) {
			AgeExp ae = r.type == JVMType.VOID ? null : operantStack.remove(operantStack.size() - 1);
			return new AbstractCommand.Return(ae).age(birth);
		} else if (um instanceof JCommand.SignCheck sc) {
			AgeExp a = operantStack.remove(operantStack.size() - 1);
			AgeExp b;
			if (sc instanceof JCommand.Compare) {
				b = a;
				a = operantStack.remove(operantStack.size() - 1);
			} else b = null;
			List<AgeExp>               params    = generateGotoParams(operantStack, lvl);
			AgeExp                     condition = new Compare(a, sc.cmp, b).age(birth);
			AbstractCommand.GotoTarget it        = new AbstractCommand.GotoTarget(jumps.get(Integer.valueOf(sc.targetAddress())));
			AbstractCommand.GotoTarget et        = new AbstractCommand.GotoTarget(jumps.get(Integer.valueOf(commandEnd)));
			return new AbstractCommand.IfGoto(condition, it, et, params).age(birth);
		} else if (um instanceof JCommand.Goto g) {
			List<AgeExp>               params = generateGotoParams(operantStack, lvl);
			AbstractCommand.GotoTarget gt     = new AbstractCommand.GotoTarget(jumps.get(Integer.valueOf(g.targetAddress())));
			return new AbstractCommand.Goto(gt, params).age(birth);
		} else if (um instanceof JCommand.LookupSwitch || um instanceof JCommand.TableSwitch) {
			List<AgeExp> params = generateGotoParams(operantStack, lvl);
			AgeExp       exp    = operantStack.remove(operantStack.size() - 1);
			return new AbstractCommand.Switch<>(exp, um, params).age(birth);
		} else {
			throw new AssertionError("unknown Command: " + um);
		}
	}
	
	private static List<AgeExp> generateGotoParams(List<AgeExp> operantStack, List<AgeExp> lvl) {
		List<AgeExp> params = new ArrayList<>();
		params.addAll(operantStack);
		params.add(AbstractCodeBuilder.PARAM_SEPERATOR);
		params.addAll(lvl);
		return params;
	}
	
	private static List<AgeExp> methodCall(List<AgeExp> operantStack, MethodType type, JCommand invokeCmd, int birth) {
		List<JType> params = type.params();
		AgeExp[]    vals   = new AgeExp[params.size()];
		for (int i = vals.length; --i >= 0;) {
			vals[i] = operantStack.remove(operantStack.size() - 1);
		}
		if (type.result() != JType.JPrimType.VOID) {
			operantStack.add(new MethodResult(type.result(), invokeCmd).age(birth));
		}
		return List.of(vals);
	}
	
}
