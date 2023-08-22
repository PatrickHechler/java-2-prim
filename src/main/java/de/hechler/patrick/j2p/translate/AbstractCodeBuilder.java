package de.hechler.patrick.j2p.translate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.hechler.patrick.j2p.parse.JMethod;

@SuppressWarnings("javadoc")
public class AbstractCodeBuilder {
	
	public final Map<Integer, ACBState>   states   = new TreeMap<>();
	public final List<AbstractCommand>    commands = new ArrayList<>();
	public final List<AbstractExpression> operantStack;
	public final AbstractExpression[]     localVariables;
	
	public AbstractCodeBuilder(JMethod method) {
		this.operantStack   = new ArrayList<>(method.maxStack());
		this.localVariables = new AbstractExpression[method.maxLocals()];
	}
	
	public ACBState generateState(int address) {
		return new ACBState(address, this.commands.size(), this.operantStack.toArray(new AbstractExpression[this.commands.size()]), this.localVariables.clone());
	}
	
	public ACBState getState(int address) {
		ACBState state = this.states.get(Integer.valueOf(address));
		if (state == null) {
			throw new AssertionError();
		}
		return state;
	}
	
	public void addState(int address) {
		ACBState old = this.states.put(Integer.valueOf(address), generateState(address));
		if (old != null) {
			throw new AssertionError();
		}
	}
	
	public static class ACBState {
		
		final int                  address;
		final int                  commandLength;
		final AbstractExpression[] operantStack;
		final AbstractExpression[] localVariables;
		
		public ACBState(int address, int commandLength, AbstractExpression[] operantStack, AbstractExpression[] localVariables) {
			this.address        = address;
			this.commandLength  = commandLength;
			this.operantStack   = operantStack;
			this.localVariables = localVariables;
		}
		
	}
	
}
