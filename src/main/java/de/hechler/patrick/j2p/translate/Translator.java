package de.hechler.patrick.j2p.translate;

import de.hechler.patrick.zeugs.pfs.interfaces.File;

@SuppressWarnings("javadoc")
public interface Translator {
	
	void addTranslationUnit(File inFile, File outFile);
	
	void translate();
	
}
