package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertyName;

import java.util.List;

public class PluginSettings {
    @PropertyName(value = "simplegit.settings.pluginActive", defaultValue = "false")
    boolean pluginActive;

    @PropertyName(value = "simplegit.settings.commonTags")
    List<String> commonTags;

    @PropertyName(value = "simplegit.settings.featurePath")
    String featurePath;

    @PropertyName(value = "simplegit.settings.gitRepositoryRootPath")
    String gitRepositoryRootPath;

    @PropertyName(value = "simplegit.settings.gitRepositoryRootPath")
    String remoteGitRepositoryURL;

    public boolean isPluginActive() {
        return pluginActive;
    }

    public List<String> getCommonTags() {
        return commonTags;
    }

    public String getFeaturePath() {
        return featurePath == null ? "" : featurePath;
    }

    public String getGitRepositoryRootPath() {
        return gitRepositoryRootPath == null ? "" : gitRepositoryRootPath;
    }

    public String getRemoteGitRepositoryURL() {
        return remoteGitRepositoryURL == null ? "" : remoteGitRepositoryURL;
    }
}
