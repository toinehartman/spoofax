package org.metaborg.spoofax.core.syntax;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.sdf2table.jsglrinterfaces.ISGLRParseTable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr2.parsetable.ParseTableReader;
import org.spoofax.terms.io.binary.TermReader;

public class JSGLR2FileParseTableProvider implements IParseTableProvider {
    private final FileObject resource;
    private final ITermFactory termFactory;

    private ISGLRParseTable parseTable;

    public JSGLR2FileParseTableProvider(FileObject resource, ITermFactory termFactory) {
        this.resource = resource;
        this.termFactory = termFactory;
    }

    @Override public ISGLRParseTable parseTable() throws IOException {
        if(parseTable != null) {
            return parseTable;
        }

        resource.refresh();
        
        if(!resource.exists()) {
            throw new IOException("Could not load parse table from " + resource + ", file does not exist");
        }

        try(final InputStream stream = resource.getContent().getInputStream()) {
            final TermReader termReader = new TermReader(termFactory);
            IStrategoTerm parseTableTerm = termReader.parseFromStream(stream);
            
            FileObject persistedTable = resource.getParent().resolveFile("table.bin");
            if(!persistedTable.exists()) {
                parseTable = ParseTableReader.read(parseTableTerm);
            } else {
                parseTable = ParseTableReader.read(parseTableTerm, persistedTable);
            }
        } catch(Exception e) {
            throw new IOException("Could not load parse table from " + resource, e);
        }

        return parseTable;
    }
}
