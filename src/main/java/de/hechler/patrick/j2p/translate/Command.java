package de.hechler.patrick.j2p.translate;


public sealed interface Command {
	
	record Assign() implements Command {}
	
	record UselessAccess() implements Command {}
	
	record MethodInvocation() implements Command {}
	
	record IfGoto(int targetIndex) implements Command {}
	
	record Goto(int targetIndex) implements Command {}
	
	record Return() implements Command {}
	
}
