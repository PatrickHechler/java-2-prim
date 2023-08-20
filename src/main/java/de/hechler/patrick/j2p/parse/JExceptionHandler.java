package de.hechler.patrick.j2p.parse;


public record JExceptionHandler(int startAddress, int endAddress, int handlerAddress, JType catchType) {
	
}
