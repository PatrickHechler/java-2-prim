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
package de.hechler.patrick.j2p.parse;

import java.util.Arrays;

@SuppressWarnings("javadoc")
public sealed interface JStackMapEntry {
	
	int offsetDelta();
	
	static final JSMEVerificationInfo[] EMPTY_ARRAY = new JSMEVerificationInfo[0];
	
	public record FullDescribtion(int offsetDelta, JSMEVerificationInfo[] locals, JSMEVerificationInfo[] stack) implements JStackMapEntry {
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + Arrays.hashCode(this.locals);
			result = prime * result + this.offsetDelta;
			result = prime * result + Arrays.hashCode(this.stack);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof FullDescribtion)) return false;
			FullDescribtion other = (FullDescribtion) obj;
			if (!Arrays.equals(this.locals, other.locals)) return false;
			if (this.offsetDelta != other.offsetDelta) return false;
			return Arrays.equals(this.stack, other.stack);
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FullDescribtion [offsetDelta=");
			builder.append(this.offsetDelta);
			builder.append(", locals=");
			builder.append(Arrays.toString(this.locals));
			builder.append(", stack=");
			builder.append(Arrays.toString(this.stack));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public record AppendedLocalsEmptyStack(int offsetDelta, JSMEVerificationInfo[] addedLocals) implements JStackMapEntry {
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + Arrays.hashCode(this.addedLocals);
			result = prime * result + this.offsetDelta;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof AppendedLocalsEmptyStack)) return false;
			AppendedLocalsEmptyStack other = (AppendedLocalsEmptyStack) obj;
			if (!Arrays.equals(this.addedLocals, other.addedLocals)) return false;
			return this.offsetDelta == other.offsetDelta;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("AppendedLocalsEmptyStack [offsetDelta=");
			builder.append(this.offsetDelta);
			builder.append(", addedLocals=");
			builder.append(Arrays.toString(this.addedLocals));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	public record SameLocalsNewStack(int offsetDelta, JSMEVerificationInfo[] stack, int removedLocals) implements JStackMapEntry {
		
		public SameLocalsNewStack(int offsetDelta) {
			this(offsetDelta, EMPTY_ARRAY, 0);
		}
		
		public SameLocalsNewStack(int offsetDelta, JSMEVerificationInfo[] stack) {
			this(offsetDelta, stack, 0);
		}
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + this.offsetDelta;
			result = prime * result + Arrays.hashCode(this.stack);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof SameLocalsNewStack)) return false;
			SameLocalsNewStack other = (SameLocalsNewStack) obj;
			if (this.offsetDelta != other.offsetDelta) return false;
			return Arrays.equals(this.stack, other.stack);
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("SameLocalsWithStack [offsetDelta=");
			builder.append(this.offsetDelta);
			builder.append(", stack=");
			builder.append(Arrays.toString(this.stack));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
}
