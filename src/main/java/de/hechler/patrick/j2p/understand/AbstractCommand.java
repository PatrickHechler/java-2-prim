// This file is part of the java-2-prim Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.j2p.understand;

import java.util.List;
import java.util.Objects;

import de.hechler.patrick.j2p.understand.CodeUnderstander.AgeExp;

@SuppressWarnings("javadoc")
public sealed interface AbstractCommand {
	
	default CodeUnderstander.AgeCmd age(int age) {
		return new CodeUnderstander.AgeCmd(this, age);
	}
	
	record Assign(AgeExp target, AgeExp value) implements AbstractCommand {
		
		public Assign {
			Objects.requireNonNull(target, "target");
			Objects.requireNonNull(value, "value");
		}
		
	}
	
	record Access(AgeExp access) implements AbstractCommand {
		
		public Access {
			Objects.requireNonNull(access, "access");
		}
		
	}
	
	record MethodInvokation<I>(I methodIdentifier, List<AgeExp> parameters) implements AbstractCommand {
		
		public MethodInvokation(I methodIdentifier, List<AgeExp> parameters) {
			this.methodIdentifier = Objects.requireNonNull(methodIdentifier, "identifier");
			this.parameters       = List.copyOf(parameters);
		}
		
	}
	
	record New(AgeExp newValue) implements AbstractCommand {
		
		public New {
			Objects.requireNonNull(newValue, "newValue");
		}
		
	}
	
	record Assert<I>(I info, AgeExp assumeValid) implements AbstractCommand {
		
		public Assert {
			Objects.requireNonNull(info, "info");
			Objects.requireNonNull(assumeValid, "assumeValid");
		}
		
	}
	
	record Return(AgeExp returnValueOrNull) implements AbstractCommand {}
	
	record Switch<I>(AgeExp value, I mapper, List<AgeExp> params) implements AbstractCommand {
		
		public Switch(AgeExp value, I mapper, List<AgeExp> params) {
			this.value  = Objects.requireNonNull(value, "value");
			this.mapper = Objects.requireNonNull(mapper, "mapper");
			this.params = List.copyOf(params);
		}
		
	}
	
	record IfGoto(AgeExp condition, GotoTarget ifTarget, GotoTarget elseTarget, List<AgeExp> params) implements AbstractCommand {
		
		public IfGoto(AgeExp condition, GotoTarget ifTarget, GotoTarget elseTarget, List<AgeExp> params) {
			this.condition  = Objects.requireNonNull(condition, "condition");
			this.ifTarget   = Objects.requireNonNull(ifTarget, "ifTarget");
			this.elseTarget = Objects.requireNonNull(elseTarget, "elseTarget");
			this.params     = List.copyOf(params);
		}
		
		
	}
	
	record Goto(GotoTarget target, List<AgeExp> params) implements AbstractCommand {
		
		public Goto(GotoTarget target, List<AgeExp> params) {
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
	
}
