package de.hechler.patrick.j2p;

import java.util.ArrayList;
import java.util.List;

public class JMethod {
	
	public final ClassFile  file;
	public final int        accessFlags;
	public final String     name;
	public final MethodType methodType;
	public int              maxStack;
	public int              maxLocals;
	public List<JCommand>   cmds;
	
	public JMethod(ClassFile file, int accessFlags, String name, MethodType methodType) {
		this.file        = file;
		this.accessFlags = accessFlags;
		this.name        = name;
		this.methodType  = methodType;
	}
	
	public void initCode(int maxStack, int maxLocals) {
		this.maxStack  = maxStack;
		this.maxLocals = maxLocals;
		this.cmds      = new ArrayList<>();
	}
	
	public void addCommand(JCommand cmd) {
		this.cmds.add(cmd);
	}
	
}
