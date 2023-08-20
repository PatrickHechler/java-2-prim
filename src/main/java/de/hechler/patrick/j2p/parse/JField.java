package de.hechler.patrick.j2p.parse;


public record JField(int accessFlags, String name, JType descriptor, CPEntry initialValue) {
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("JField [accessFlags=").append(this.accessFlags).append(" : 0x").append(Integer.toHexString(this.accessFlags)).append(" : ")
				.append(Modifier.fieldString(this.accessFlags)).append(", name=").append(this.name).append(", descriptor=").append(this.descriptor);
		if (this.initialValue != null) {
			b.append(", initialValue=").append(this.initialValue);
		}
		return b.append(']').toString();
	}
	
}
