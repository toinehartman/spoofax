package org.metaborg.core.config;

import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.metaborg.core.language.LanguageContributionIdentifier;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageBuilder;

import com.google.common.collect.Lists;

/**
 * An implementation of the {@link ILanguageComponentConfig} interface that is backed by an
 * {@link ImmutableConfiguration} object.
 */
public class LanguageComponentConfig extends ProjectConfig implements ILanguageComponentConfig, IConfig {
    private static final String PROP_IDENTIFIER = "id";
    private static final String PROP_NAME = "name";
    private static final String PROP_LANGUAGE_CONTRIBUTIONS = "contributions";
    private static final String PROP_GENERATES = "generates";
    private static final String PROP_EXPORTS = "exports";

    public LanguageComponentConfig(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
    }

    protected LanguageComponentConfig(HierarchicalConfiguration<ImmutableNode> config, String metaborgVersion,
        LanguageIdentifier identifier, String name, Collection<LanguageIdentifier> compileDeps,
        Collection<LanguageIdentifier> sourceDeps, Collection<LanguageIdentifier> javaDeps, boolean typesmart,
        Collection<LanguageContributionIdentifier> langContribs, Collection<IGenerateConfig> generates,
        Collection<IExportConfig> exports) {
        super(config, metaborgVersion, compileDeps, sourceDeps, javaDeps, typesmart);

        config.setProperty(PROP_NAME, name);
        config.setProperty(PROP_IDENTIFIER, identifier);
        config.setProperty(PROP_LANGUAGE_CONTRIBUTIONS, langContribs);
        config.setProperty(PROP_GENERATES, generates);
        config.setProperty(PROP_EXPORTS, exports);
    }


    @Override public LanguageIdentifier identifier() {
        return config.get(LanguageIdentifier.class, PROP_IDENTIFIER);
    }

    @Override public String name() {
        return config.getString(PROP_NAME);
    }

    @Override public Collection<LanguageContributionIdentifier> langContribs() {
        final List<HierarchicalConfiguration<ImmutableNode>> langContribConfigs =
            config.configurationsAt(PROP_LANGUAGE_CONTRIBUTIONS);
        final List<LanguageContributionIdentifier> langContribs =
            Lists.newArrayListWithCapacity(langContribConfigs.size());
        for(HierarchicalConfiguration<ImmutableNode> langContribConfig : langContribConfigs) {
            // HACK: for some reason get(LanguageIdentifier.class, "id") does not work here, it cannot convert to a
            // language identifier, do manually instead.
            final String idString = langContribConfig.getString("id");
            final LanguageIdentifier id = LanguageIdentifier.parse(idString);
            final String name = langContribConfig.getString("name");
            langContribs.add(new LanguageContributionIdentifier(id, name));
        }
        return langContribs;
    }

    @Override public Collection<IGenerateConfig> generates() {
        final List<HierarchicalConfiguration<ImmutableNode>> generateConfigs = config.configurationsAt(PROP_GENERATES);
        final List<IGenerateConfig> generates = Lists.newArrayListWithCapacity(generateConfigs.size());
        for(HierarchicalConfiguration<ImmutableNode> generateConfig : generateConfigs) {
            final String language = generateConfig.getString("language");
            final String directory = generateConfig.getString("directory");
            generates.add(new GenerateConfig(language, directory));
        }
        return generates;
    }

    @Override public Collection<IExportConfig> exports() {
        final List<HierarchicalConfiguration<ImmutableNode>> exportConfigs = config.configurationsAt(PROP_EXPORTS);
        final List<IExportConfig> exports = Lists.newArrayListWithCapacity(exportConfigs.size());
        for(HierarchicalConfiguration<ImmutableNode> exportConfig : exportConfigs) {
            final String languageName = exportConfig.getString("language");
            final String directory = exportConfig.getString("directory");
            final String file = exportConfig.getString("file");
            final List<String> includes = exportConfig.getList(String.class, "includes", Lists.<String>newArrayList());
            final List<String> excludes = exportConfig.getList(String.class, "excludes", Lists.<String>newArrayList());
            if(languageName != null) {
                if(directory != null) {
                    exports.add(new LangDirExport(languageName, directory, includes, excludes));
                } else if(file != null) {
                    exports.add(new LangFileExport(languageName, file));
                }
            } else {
                if(directory != null) {
                    exports.add(new ResourceExport(directory, includes, excludes));
                }
            }
        }
        return exports;
    }

    public Collection<IMessage> validate(MessageBuilder mb) {
        final Collection<IMessage> messages = super.validate(mb);
        final String idStr = config.getString(PROP_IDENTIFIER);
        if(idStr == null) {
            messages.add(mb.withMessage("Field 'id' must be set").build());
        } else {
            try {
                LanguageIdentifier.parseFull(idStr);
            } catch(IllegalArgumentException e) {
                messages.add(mb.withMessage("Invalid identifier in 'id' field. " + e.getMessage()).build());
            }
        }

        final String name = config.getString(PROP_NAME);
        if(name == null) {
            messages.add(mb.withMessage("Field 'name' must be set").build());
        } else {
            if(!LanguageIdentifier.validId(name)) {
                messages.add(
                    mb.withMessage("Field 'name' contains invalid characters, " + LanguageIdentifier.errorDescription)
                        .build());
            }
        }

        // TODO: validate language contributions
        // TODO: validate generates
        // TODO: validate exports

        return messages;
    }
}
