package de.hechler.patrick.j2p;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hechler.patrick.j2p.parse.AlignableDataInput;
import de.hechler.patrick.j2p.parse.ClassFile;
import de.hechler.patrick.j2p.parse.ClassReader;
import de.hechler.patrick.j2p.parse.JMethod;
import de.hechler.patrick.j2p.understand.AbstractCodeBuilder;
import de.hechler.patrick.j2p.understand.CodeUnderstander;

@SuppressWarnings("javadoc")
public class J2PMain {
	
	private static final int     MAJOR           = 1;
	private static final int     MINOR           = 0;
	private static final int     FIX             = 0;
	private static final boolean SNAPSHOT        = true;
	private static final String  VERSION_STRING  = MAJOR + "." + MINOR + "." + FIX + (SNAPSHOT ? "-SNAPSHOT" : "");
	private static final String  JAVA_SE_VERSION = "JavaSE 20";
	
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			help(System.out);
			return;
		}
		List<String> files = new ArrayList<>();
		for (String a : args) {
			if ("--help".equals(a)) {
				help(System.out);
			} else if ("--version".equals(a)) {
				version(System.out);
			} else {
				files.add(a);
			}
		}
		ClassReader reader = new ClassReader();
		CodeUnderstander cu = new CodeUnderstander();
		for (String file : files) {
			try (AlignableDataInput in = new AlignableDataInput(new FileInputStream(file))) {
				System.out.println("parse now: " + file);
				ClassFile f = reader.read(in);
				System.out.println(f);
				for (JMethod m : f.methods()) {
					System.out.println();
					System.out.println("understand now: " + f.thisClass());
					System.out.println("       " + m.name + " : " + m.methodType);
					Map<Integer, AbstractCodeBuilder> map = cu.understand(m);
					for (AbstractCodeBuilder acb : map.values()) {
						acb.print(System.out);
					}
				}
			}
		}
	}
	
	private static void version(PrintStream out) {
		out.print("java2prim: version " + VERSION_STRING + "\n"//
				+ "the maximum and recommended java version is " + JAVA_SE_VERSION + "\n");
	}
	
	private static void help(PrintStream out) {
		out.print("""
				java2prim help:
				Usage: java2prim <options>
				options:
				    --help: print this message
				    ---version: print version information
				    <file>: parse the given class file
				""");
	}
	
}
