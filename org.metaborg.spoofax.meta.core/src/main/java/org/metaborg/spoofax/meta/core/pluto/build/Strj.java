package org.metaborg.spoofax.meta.core.pluto.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.metaborg.spoofax.meta.core.pluto.SpoofaxBuilder;
import org.metaborg.spoofax.meta.core.pluto.SpoofaxBuilderFactory;
import org.metaborg.spoofax.meta.core.pluto.SpoofaxBuilderFactoryFactory;
import org.metaborg.spoofax.meta.core.pluto.SpoofaxContext;
import org.metaborg.spoofax.meta.core.pluto.SpoofaxInput;
import org.metaborg.spoofax.meta.core.pluto.StrategoExecutor;
import org.metaborg.spoofax.meta.core.pluto.StrategoExecutor.ExecutionResult;
import org.metaborg.spoofax.meta.core.pluto.util.LoggingFilteringIOAgent;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;

import build.pluto.BuildUnit.State;
import build.pluto.builder.BuildRequest;
import build.pluto.dependency.Origin;
import build.pluto.output.None;

public class Strj extends SpoofaxBuilder<Strj.Input, None> {
    public static class Input extends SpoofaxInput {
        private static final long serialVersionUID = -5234502421638344690L;

        public final File inputPath;
        public final File outputPath;
        public final File depPath;
        public final String packageName;
        public final boolean library;
        public final boolean clean;
        public final File[] directoryIncludes;
        public final String[] libraryIncludes;
        public final File cacheDir;
        public final String[] additionalArgs;
        public final Origin requiredUnits;


        public Input(SpoofaxContext context, File inputPath, File outputPath, File depPath, String packageName,
            boolean library, boolean clean, File[] directoryIncludes, String[] libraryIncludes, File cacheDir,
            String[] additionalArgs, Origin requiredUnits) {
            super(context);
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.depPath = depPath;
            this.packageName = packageName;
            this.library = library;
            this.clean = clean;
            this.directoryIncludes = directoryIncludes;
            this.libraryIncludes = libraryIncludes;
            this.cacheDir = cacheDir;
            this.additionalArgs = additionalArgs;
            this.requiredUnits = requiredUnits;
        }
    }


    public static SpoofaxBuilderFactory<Input, None, Strj> factory = SpoofaxBuilderFactoryFactory.of(Strj.class,
        Input.class);


    public Strj(Input input) {
        super(input);
    }


    public static BuildRequest<Input, None, Strj, SpoofaxBuilderFactory<Input, None, Strj>> request(Input input) {
        return new BuildRequest<>(factory, input);
    }

    public static Origin origin(Input input) {
        return Origin.from(request(input));
    }


    @Override protected String description(Input input) {
        return "Compile Stratego code";
    }

    @Override public File persistentPath(Input input) {
        final Path rel = FileCommands.getRelativePath(context.baseDir, input.inputPath);
        final String relname = rel.toString().replace(File.separatorChar, '_');
        return context.depPath("strj." + relname + ".dep");
    }

    @Override public None build(Input input) throws IOException {
        requireBuild(input.requiredUnits);

        require(input.inputPath);

        final File rtree = FileCommands.replaceExtension(input.outputPath, "rtree");
        final File strdep = FileCommands.addExtension(input.outputPath, "dep");

        FileCommands.delete(rtree);

        final StringBuilder directoryIncludes = new StringBuilder();
        for(File dir : input.directoryIncludes) {
            if(dir != null) {
                directoryIncludes.append("-I ").append(dir).append(" ");
            }
        }
        final StringBuilder libraryIncludes = new StringBuilder();
        for(String lib : input.libraryIncludes) {
            if(lib != null && !lib.isEmpty()) {
                directoryIncludes.append("-la ").append(lib).append(" ");
            }
        }

        final ExecutionResult result =
            StrategoExecutor.runStrategoCLI(
                StrategoExecutor.strjContext(),
                org.strategoxt.strj.main_0_0.instance,
                "strj",
                new LoggingFilteringIOAgent(Pattern.quote("[ strj | info ]") + ".*", Pattern
                    .quote("[ strj | error ] Compilation failed") + ".*", Pattern
                    .quote("[ strj | warning ] Nullary constructor") + ".*"), "-i", input.inputPath, "-o",
                input.outputPath, input.packageName != null ? "-p " + input.packageName : "", input.library
                    ? "--library" : "", input.clean ? "--clean" : "", directoryIncludes, libraryIncludes,
                input.cacheDir != null ? "--cache-dir " + input.cacheDir : "", StringCommands.printListSeparated(
                    input.additionalArgs, " "));

        FileCommands.delete(rtree);

        if(input.depPath.isDirectory()) {
            for(Path sourceFile : FileCommands.listFilesRecursive(input.depPath.toPath())) {
                provide(sourceFile.toFile());
            }
        } else {
            provide(input.depPath);
        }
        provide(rtree);
        provide(strdep);

        if(FileCommands.exists(strdep)) {
            registerUsedPaths(strdep);
        }

        setState(State.finished(result.success));

        return None.val;
    }

    private void registerUsedPaths(File strdep) throws IOException {
        final List<String> lines = org.apache.commons.io.FileUtils.readLines(strdep);

        // Skip first line (start at 1 instead of 0), which lists the generated CTree file.
        for(int i = 1; i < lines.size(); i++) {
            // Remove leading and trailing whitespace.
            final String trimmedLine = lines.get(i).trim();
            final int length = trimmedLine.length();
            if(length < 3) {
                // Don't process empty lines, i.e. lines with just ' /' or '/'.
                continue;
            }
            // Remove the trailing ' /'.
            final String line = trimmedLine.substring(0, length - 2);

            // Make paths relative, if possible, to prevent rebuilds when they are moved to another location. Paths 
            // outside of the base directory will be required as absolute paths.
            
            // TODO: non-local dependencies, such as those on .spoofax-language files, are copied to a temporary
            // directory. That will cause unnecessary rebuilds because of absolute path dependencies.
            final File path = new File(line);
            final Path relativePath = FileCommands.getRelativePath(context.baseDir, path);
            require(relativePath != null ? relativePath.toFile() : path);
        }
    }
}
