package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertyName;
import ru.lanit.ideaplugin.simplegit.tags.model.EditableCommonTagList;
import ru.lanit.ideaplugin.simplegit.tags.model.FixedCommonTagList;
import ru.lanit.ideaplugin.simplegit.tags.tag.CommonTag;

import java.util.List;

public class PluginSettings {
    @PropertyName(value = "simplegit.settings.pluginActive", defaultValue = "false")
    private boolean pluginActive;

    @PropertyName(value = "simplegit.settings.commonTagsList")
    private String commonTags;

    @PropertyName(value = "simplegit.settings.featurePath")
    private String featurePath;

    @PropertyName(value = "simplegit.settings.gitRepositoryRootPath")
    private String gitRepositoryRootPath;

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
