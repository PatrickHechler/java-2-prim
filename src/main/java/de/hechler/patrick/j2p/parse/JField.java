package de.hechler.patrick.j2p.parse;

import java.lang.reflect.Modifier;

public record JField(int accessFlags, String name, JType descriptor, Object initialValue) {
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("JField [accessFlags=").append(this.accessFlags).append(" : 0x").append(Integer.toHexString(this.accessFlags)).append(" : ")
				.append(Modifier.toString(this.accessFlags)).append(", name=").append(this.name).append(", descriptor=").append(this.descriptor);
		if (this.initialValue != null) {
			b.append(", initialValue=").append(this.initialValue);
		}
		return b.append(']').toString();
	}
	
}
