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
	
	record New(AbstractExpression newValue) implements AbstractCommand {
		
		public New {
			Objects.requireNonNull(newValue, "newValue");
		}
		
	}
	
	record Assert<I>(I info, AbstractExpression assumeValid) implements AbstractCommand {
		
		public Assert {
			Objects.requireNonNull(info, "info");
			Objects.requireNonNull(assumeValid, "assumeValid");
		}
		
	}
	
	record Switch<I>(AbstractExpression value, I mapper, List<AbstractExpression> params) implements AbstractCommand {
		
		public Switch(AbstractExpression value, I mapper, List<AbstractExpression> params) {
			this.value  = Objects.requireNonNull(value, "value");
			this.mapper = Objects.requireNonNull(mapper, "mapper");
			this.params = List.copyOf(params);
		}
		
	}
	
	record IfGoto(AbstractExpression condition, GotoTarget ifTarget, GotoTarget elseTarget, List<AbstractExpression> params) implements AbstractCommand {
		
		public IfGoto(AbstractExpression condition, GotoTarget ifTarget, GotoTarget elseTarget, List<AbstractExpression> params) {
			this.condition  = Objects.requireNonNull(condition, "condition");
			this.ifTarget   = Objects.requireNonNull(ifTarget, "ifTarget");
			this.elseTarget = Objects.requireNonNull(elseTarget, "elseTarget");
			this.params     = List.copyOf(params);
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
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((this.target == null) ? 0 : this.target.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof GotoTarget)) return false;
			GotoTarget other = (GotoTarget) obj;
			if (this.target == null) {
				if (other.target != null) return false;
			} else if (!this.target.equals(other.target)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("GotoTarget [target=");
			builder.append(this.target);
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	record Return(AbstractExpression returnValueOrNull) implements AbstractCommand {}
	
}
