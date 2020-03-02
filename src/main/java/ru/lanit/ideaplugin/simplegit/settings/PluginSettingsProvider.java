package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import ru.lanit.ideaplugin.simplegit.dialogs.newfeature.NewFeatureDialog;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;
import ru.lanit.ideaplugin.simplegit.tags.tag.CommonTag;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PluginSettingsProvider {
    private final PropertiesComponent properties;
    private final SettingsChangeListener changeListener;
    private PluginSettings settings;

    public PluginSettingsProvider(Project project, SettingsChangeListener changeListener) {
        properties = PropertiesComponent.getInstance(project);
        this.changeListener = changeListener;
    }

    public boolean restoreAllSettings() {
        PluginSettings oldSettings = settings;
        settings = new PluginSettings();
        if (properties.loadFields(settings)) {
            cleanupSettings(settings);
            changeListener.onSettingsChange(settings, oldSettings);
            return true;
        }
        settings = oldSettings;
        return false;
    }

    private boolean saveAllSettings() {
        return properties.saveFields(settings);
    }

    private void cleanupSettings(PluginSettings settings) {
        if (isNull(settings.featurePath)) {
            settings.featurePath = "";
        }
        if (isNull(settings.gitRepositoryRootPath)) {
            settings.gitRepositoryRootPath = "";
        }
        if (isNull(settings.remoteGitRepositoryURL)) {
            settings.remoteGitRepositoryURL = "";
        }
    }

    private boolean isNull(String value) {
        return (value == null || value.equals("null"));
    }

    public void setSettingsFromDialog(PluginSettingsDialog pluginSettingsDialog) {
        PluginSettings oldSettings = settings;
        settings = new PluginSettings();
        settings.pluginActive = pluginSettingsDialog.isPluginActive();
        settings.commonTags = pluginSettingsDialog.getCommonTags().isEmpty()
                ? null
                : Arrays.stream(pluginSettingsDialog.getCommonTags().split(";")).map(CommonTag::new).collect(Collectors.toList());
        settings.featurePath = pluginSettingsDialog.getFeaturePath();
        settings.gitRepositoryRootPath = pluginSettingsDialog.getGitRepositoryRootPath();
        settings.remoteGitRepositoryURL = pluginSettingsDialog.getRemoteGitRepositoryURL();
        saveAllSettings();
        changeListener.onSettingsChange(settings, oldSettings);
    }

    public void setSettingsToDialog(PluginSettingsDialog pluginSettingsDialog) {
        pluginSettingsDialog.setPluginActive(settings.pluginActive);
        pluginSettingsDialog.setCommonTags(settings.commonTags == null
                ? ""
                : settings.commonTags.stream().map(CommonTag::toString).collect(Collectors.joining(";"))
        );
        pluginSettingsDialog.setFeaturePath(settings.featurePath);
        pluginSettingsDialog.setGitRepositoryRootPath(settings.gitRepositoryRootPath);
        pluginSettingsDialog.setRemoteGitRepositoryURL(settings.remoteGitRepositoryURL);
    }

    public void setSettingsToNewFeatureDialog(NewFeatureDialog newFeatureDialog) {
        newFeatureDialog.setCommonTags(settings.commonTags);
    }

    public boolean isPluginActive() {
        return settings.pluginActive;
    }

    public List<CommonTag> getCommonTags() {
        return settings.commonTags;
    }

    public String getFeaturePath() {
        return settings.featurePath;
    }

    public String getGitRepositoryRootPath() {
        return settings.gitRepositoryRootPath;
    }

    public String getRemoteGitRepositoryURL() {
        return settings.remoteGitRepositoryURL;
    }

    public void setRemoteGitRepositoryURL(String remoteGitRepositoryURL) {
        settings.remoteGitRepositoryURL = remoteGitRepositoryURL;
    }
}
