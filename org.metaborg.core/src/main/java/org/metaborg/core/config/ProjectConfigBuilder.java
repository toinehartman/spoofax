package org.metaborg.core.config;

import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgConstants;
import org.metaborg.core.language.LanguageIdentifier;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.virtlink.commons.configuration2.jackson.JacksonConfiguration;

/**
 * Configuration-based builder for {@link ILanguageComponentConfig} objects.
 */
public class ProjectConfigBuilder implements IProjectConfigBuilder {
    protected final AConfigurationReaderWriter configReaderWriter;

    protected String metaborgVersion = MetaborgConstants.METABORG_VERSION;
    protected final Set<LanguageIdentifier> compileDeps = Sets.newHashSet();
    protected final Set<LanguageIdentifier> sourceDeps = Sets.newHashSet();
    protected final Set<LanguageIdentifier> javaDeps = Sets.newHashSet();
    protected boolean typesmart = false;

    @Inject public ProjectConfigBuilder(AConfigurationReaderWriter configReaderWriter) {
        this.configReaderWriter = configReaderWriter;
    }


    @Override public IProjectConfig build(@Nullable FileObject rootFolder) {
        final JacksonConfiguration configuration = configReaderWriter.create(null, rootFolder);

        return new ProjectConfig(configuration, metaborgVersion, compileDeps, sourceDeps, javaDeps, typesmart);
    }

    @Override public IProjectConfigBuilder reset() {
        metaborgVersion = MetaborgConstants.METABORG_VERSION;
        compileDeps.clear();
        sourceDeps.clear();
        javaDeps.clear();
        typesmart = false;
        return this;
    }

    @Override public IProjectConfigBuilder copyFrom(IProjectConfig config) {
        withMetaborgVersion(config.metaborgVersion());
        withCompileDeps(config.compileDeps());
        withSourceDeps(config.sourceDeps());
        withJavaDeps(config.javaDeps());
        withTypesmart(config.typesmart());
        return this;
    }


    @Override public IProjectConfigBuilder withMetaborgVersion(String metaborgVersion) {
        this.metaborgVersion = metaborgVersion;
        return this;
    }

    @Override public IProjectConfigBuilder withCompileDeps(Iterable<LanguageIdentifier> deps) {
        this.compileDeps.clear();
        addCompileDeps(deps);
        return this;
    }

    @Override public IProjectConfigBuilder addCompileDeps(Iterable<LanguageIdentifier> deps) {
        Iterables.addAll(this.compileDeps, deps);
        return this;
    }

    @Override public IProjectConfigBuilder withSourceDeps(Iterable<LanguageIdentifier> deps) {
        this.sourceDeps.clear();
        addSourceDeps(deps);
        return this;
    }

    @Override public IProjectConfigBuilder addSourceDeps(Iterable<LanguageIdentifier> deps) {
        Iterables.addAll(this.sourceDeps, deps);
        return this;
    }

    @Override public IProjectConfigBuilder withJavaDeps(Iterable<LanguageIdentifier> deps) {
        this.javaDeps.clear();
        addJavaDeps(deps);
        return this;
    }

    @Override public IProjectConfigBuilder withTypesmart(boolean typesmart) {
        this.typesmart = typesmart;
        return this;
    }

    @Override public IProjectConfigBuilder addJavaDeps(Iterable<LanguageIdentifier> deps) {
        Iterables.addAll(this.javaDeps, deps);
        return this;
    }
}
