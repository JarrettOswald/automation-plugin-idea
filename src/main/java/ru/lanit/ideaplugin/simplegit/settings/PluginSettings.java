package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertyName;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NonNls;

import java.util.Locale;

public class PluginSettings {
    private static final Logger log = Logger.getInstance(PluginSettings.class);
    @NonNls public static final String SIMPLEGIT_SETTINGS_LOCALE = "simplegit.settings.locale";

    @PropertyName(value = "simplegit.settings.remoteGitRepositoryURL")
    private String remoteGitRepositoryURL;

    public boolean isPluginActive() {
        return pluginActive;
    }

    public void setPluginActive(boolean pluginActive) {
        this.pluginActive = pluginActive;
    }

    public EditableCommonTagList getCommonTags() {
        if (commonTags == null) {
            return new EditableCommonTagList();
        }
        return new EditableCommonTagList(commonTags.split(";"));
    }

    public void setCommonTags(EditableCommonTagList commonTags) {
        this.commonTags = commonTags == null ? "" : commonTags.toString();
    }

    public String getFeaturePath() {
        return featurePath == null ? "" : featurePath;
    }

    public void setFeaturePath(String featurePath) {
        this.featurePath = featurePath == null ? "" : featurePath;
    }

    public String getGitRepositoryRootPath() {
        return gitRepositoryRootPath == null ? "" : gitRepositoryRootPath;
    }

    public void setGitRepositoryRootPath(String gitRepositoryRootPath) {
        this.gitRepositoryRootPath = gitRepositoryRootPath == null ? "" : gitRepositoryRootPath;
    }

    public String getRemoteGitRepositoryURL() {
        return remoteGitRepositoryURL == null ? "" : remoteGitRepositoryURL;
    }

    public void setRemoteGitRepositoryURL(String remoteGitRepositoryURL) {
        this.remoteGitRepositoryURL = remoteGitRepositoryURL == null ? "" : remoteGitRepositoryURL;
    }

    public void cleanup() {
        this.commonTags = commonTags == null ? "" : commonTags;
        this.featurePath = featurePath == null ? "" : featurePath;
        this.gitRepositoryRootPath = gitRepositoryRootPath == null ? "" : gitRepositoryRootPath;
        this.remoteGitRepositoryURL = remoteGitRepositoryURL == null ? "" : remoteGitRepositoryURL;
    }
}
