package de.hechler.patrick.j2p.translate;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("javadoc")
public sealed interface AbstractCommand {
	
	record Assign(AbstractExpression target, AbstractExpression value) implements AbstractCommand {
		
		public Assign {
			Objects.requireNonNull(target, "target");
			Objects.requireNonNull(value, "value");
		}
		
	}
	
	record Access(AbstractExpression.AccessableValue access) implements AbstractCommand {
		
		public Access {
			Objects.requireNonNull(access, "access");
		}
		
	}
	
	record MethodInvokation<I>(I methodIdentifier, List<AbstractExpression> parameters) implements AbstractCommand {
		
		public MethodInvokation(I methodIdentifier, List<AbstractExpression> parameters) {
			this.methodIdentifier = Objects.requireNonNull(methodIdentifier, "identifier");
			this.parameters       = List.copyOf(parameters);
		}
		
	}
	
	record IfGoto(AbstractExpression condition, GotoTarget ifTarget, GotoTarget elseTarget, List<AbstractExpression> params) implements AbstractCommand {
		
		public IfGoto {
			Objects.requireNonNull(condition, "condition");
			Objects.requireNonNull(ifTarget, "ifTarget");
			Objects.requireNonNull(elseTarget, "elseTarget");
		}
		
		
	}
	
	record Goto(GotoTarget target, List<AbstractExpression> params) implements AbstractCommand {
		
		public Goto(GotoTarget target, List<AbstractExpression> params) {
			this.target = Objects.requireNonNull(target, "target");
			this.params = List.copyOf(params);
		}
		
	}
	
	final class GotoTarget {
		
		private AbstractCodeBuilder target;
		
		public GotoTarget(AbstractCodeBuilder target) {
			this.target = Objects.requireNonNull(target, "target");
		}
		
		public AbstractCodeBuilder nonFinalTarget() {
			if (this.target == null) {
				throw new AssertionError("the target was already finalized");
			}
			return this.target;
		}
		
	}
	
	record Return(AbstractExpression returnValueOrNull) implements AbstractCommand {}
	
}
