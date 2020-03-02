package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertyName;
import ru.lanit.ideaplugin.simplegit.tags.tag.CommonTag;

import java.util.List;

public class PluginSettings {
    @PropertyName(value = "simplegit.settings.pluginActive", defaultValue = "false")
    boolean pluginActive;

    @PropertyName(value = "simplegit.settings.commonTagsList")
    List<CommonTag> commonTags;

    @PropertyName(value = "simplegit.settings.featurePath")
    String featurePath;

    @PropertyName(value = "simplegit.settings.gitRepositoryRootPath")
    String gitRepositoryRootPath;

    @PropertyName(value = "simplegit.settings.remoteGitRepositoryURL")
    String remoteGitRepositoryURL;

    public boolean isPluginActive() {
        return pluginActive;
    }

    public List<CommonTag> getCommonTags() {
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
