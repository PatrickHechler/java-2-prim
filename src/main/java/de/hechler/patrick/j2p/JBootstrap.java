package de.hechler.patrick.j2p;

import java.util.Arrays;

@SuppressWarnings("javadoc")
public sealed interface JBootstrap {
	
	CPEntry entry();
	
	CPEntry[] arguments();
	
	record Dynamic(CPEntry.CPEDynamic entry, CPEntry[] arguments) implements JBootstrap {
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + Arrays.hashCode(this.arguments);
			result = prime * result + ((this.entry == null) ? 0 : this.entry.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof Dynamic)) return false;
			Dynamic other = (Dynamic) obj;
			if (!Arrays.equals(this.arguments, other.arguments)) return false;
			if (this.entry == null) {
				if (other.entry != null) return false;
			} else if (!this.entry.equals(other.entry)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Dynamic [entry=");
			builder.append(this.entry);
			builder.append(", arguments=");
			builder.append(Arrays.toString(this.arguments));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	record InvokeDynamic(CPEntry.CPEInvokeDynamic entry, CPEntry[] arguments) implements JBootstrap {
		
		@Override
		public int hashCode() {
			final int prime  = 31;
			int       result = 1;
			result = prime * result + Arrays.hashCode(this.arguments);
			result = prime * result + ((this.entry == null) ? 0 : this.entry.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof InvokeDynamic)) return false;
			InvokeDynamic other = (InvokeDynamic) obj;
			if (!Arrays.equals(this.arguments, other.arguments)) return false;
			if (this.entry == null) {
				if (other.entry != null) return false;
			} else if (!this.entry.equals(other.entry)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("InvokeDynamic [entry=");
			builder.append(this.entry);
			builder.append(", arguments=");
			builder.append(Arrays.toString(this.arguments));
			builder.append("]");
			return builder.toString();
		}
		
	}
	
}
