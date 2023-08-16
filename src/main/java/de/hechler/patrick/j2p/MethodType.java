package de.hechler.patrick.j2p;

import java.util.List;

public record MethodType(List<JType> params, JType result) {
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append('(');
		for (JType p : this.params) {
			b.append(p);
		}
		b.append(')');
		b.append(this.result);
		return b.toString();
	}
	
}
