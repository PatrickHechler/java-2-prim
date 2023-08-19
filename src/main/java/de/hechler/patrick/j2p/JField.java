package de.hechler.patrick.j2p;


public record JField(int accessFlags, String name, JType descriptor, Object initialValue) {
	
}
