package de.hechler.patrick.j2p.translate;

import java.util.ArrayList;
import java.util.List;

import de.hechler.patrick.j2p.parse.JCommand;
import de.hechler.patrick.j2p.parse.JMethod;
import de.hechler.patrick.j2p.parse.JSMEVerificationInfo;
import de.hechler.patrick.j2p.parse.JStackMapEntry;
import de.hechler.patrick.j2p.parse.JType;

@SuppressWarnings("javadoc")
public class AbstractCodeBuilder {
	
	public final List<AbstractCommand>    commands = new ArrayList<>();
	public final List<AbstractExpression> operantStack;
	public final AbstractExpression[]     localVariables;
	private boolean                       initilized;
	
	public AbstractCodeBuilder(JMethod method) {
		this.operantStack   = new ArrayList<>(method.maxStack());
		this.localVariables = new AbstractExpression[method.maxLocals()];
	}
	
	public void initParameters(JSMEVerificationInfo[] locs, List<JSMEVerificationInfo> stack, JMethod method) {
		if (this.initilized) {
			throw new AssertionError("already initilized");
		}
		this.initilized = true;
		int pi = 0;
		for (int i = 0; i < locs.length; i++) {
			JSMEVerificationInfo loc  = locs[i];
			JType                type = typeOfParam(method, loc);
			if (type == null) continue;
			this.localVariables[i] = new CodeUnderstander.Parameter(pi++, type);
		}
		for (int i = 0; i < stack.size(); i++) {
			JSMEVerificationInfo loc  = stack.get(i);
			JType                type = typeOfParam(method, loc);
			if (type == null) continue;
			this.operantStack.add(new CodeUnderstander.Parameter(pi++, type));
		}
	}
	
	public void initParameters(JStackMapEntry.FullDescribtion parametrs, JMethod method) {
		if (this.initilized) {
			throw new AssertionError("already initilized");
		}
		this.initilized = true;
		int                    pi   = 0;
		JSMEVerificationInfo[] locs = parametrs.locals();
		for (int i = 0; i < locs.length; i++) {
			JSMEVerificationInfo loc  = locs[i];
			JType                type = typeOfParam(method, loc);
			if (type == null) continue;
			this.localVariables[i] = new CodeUnderstander.Parameter(pi++, type);
		}
		JSMEVerificationInfo[] stack = parametrs.stack();
		for (int i = 0; i < stack.length; i++) {
			JSMEVerificationInfo loc  = stack[i];
			JType                type = typeOfParam(method, loc);
			if (type == null) continue;
			this.operantStack.add(new CodeUnderstander.Parameter(pi++, type));
		}
	}
	
	public boolean initilized() {
		return this.initilized;
	}
	
	private static JType typeOfParam(JMethod method, JSMEVerificationInfo loc) throws AssertionError {
		if (loc instanceof JSMEVerificationInfo.SimpleInfo si) {
			switch (si) {
			case DOUBLE:
				return JType.JPrimType.DOUBLE;
			case FLOAT:
				return JType.JPrimType.FLOAT;
			case INTEGER:
				return JType.JPrimType.INT;
			case LONG:
				return JType.JPrimType.LONG;
			case UNINITIALIZEDTHIS:
				return new JType.UninitMe(((JType.ObjectType) method.file.thisClass()).binaryName());
			case NULL:
				return JType.NullType.TYPE;
			case TOP:
				return null;
			default:
				throw new AssertionError("unknown SimpleInfo type: " + si.name());
			}
		} else if (loc instanceof JSMEVerificationInfo.ObjectInfo oi) {
			return oi.type();
		} else if (loc instanceof JSMEVerificationInfo.UninitilizedInfo ui) {
			return new JType.UninitObjectType(ui.newAddress(), findUninitType(method, ui.newAddress()));
		} else {
			throw new AssertionError("unknown JSMEVerificationInfo type: " + loc.getClass());
		}
	}
	
	
	private static JType findUninitType(JMethod method, int newAddress) {
		List<JCommand> cs = method.commands();
		long           na = newAddress & 0xFFFFFFFFL;
		for (int i = 0;; i++) {
			JCommand c = cs.get(i);
			if (c.address() < na) continue;
			if (c.address() != na) throw new ClassFormatError("the newAddress does not exist");
			if (!(c instanceof JCommand.New n)) throw new ClassFormatError("the command at the new address is no new instruction");
			return n.type;
		}
	}
	
}
