package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;

import java.util.Arrays;
import java.util.List;

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
                : Arrays.asList(pluginSettingsDialog.getCommonTags().split(";"));
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
                : String.join(";", settings.commonTags)
        );
        pluginSettingsDialog.setFeaturePath(settings.featurePath);
        pluginSettingsDialog.setGitRepositoryRootPath(settings.gitRepositoryRootPath);
        pluginSettingsDialog.setRemoteGitRepositoryURL(settings.remoteGitRepositoryURL);
    }

    public boolean isPluginActive() {
        return settings.pluginActive;
    }

    public List<String> getCommonTags() {
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
