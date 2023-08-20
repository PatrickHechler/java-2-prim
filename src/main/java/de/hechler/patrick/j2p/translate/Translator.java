package de.hechler.patrick.j2p.translate;

import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;

@SuppressWarnings("javadoc")
public interface Translator {
	
	void initilizeTranslator(Folder outFolder);
	
	void addTranslationUnit(File inFile);
	
	void translate();
	
}
