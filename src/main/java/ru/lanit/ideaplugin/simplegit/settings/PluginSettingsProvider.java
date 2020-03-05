package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import ru.lanit.ideaplugin.simplegit.dialogs.newfeature.NewFeatureDialog;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;
import ru.lanit.ideaplugin.simplegit.tags.model.EditableCommonTagList;
import ru.lanit.ideaplugin.simplegit.tags.model.FixedCommonTagList;

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
            settings.cleanup();
            changeListener.onSettingsChange(settings, oldSettings);
            return true;
        }
        settings = oldSettings;
        return false;
    }

    private boolean saveAllSettings() {
        return properties.saveFields(settings);
    }

    private boolean isNull(String value) {
        return (value == null || value.equals("null"));
    }

    public void setSettingsFromDialog(PluginSettingsDialog pluginSettingsDialog) {
        PluginSettings oldSettings = settings;
        settings = new PluginSettings();
        settings.setPluginActive(pluginSettingsDialog.isPluginActive());
        settings.setCommonTags(pluginSettingsDialog.getCommonTags());
        settings.setFeaturePath(pluginSettingsDialog.getFeaturePath());
        settings.setGitRepositoryRootPath(pluginSettingsDialog.getGitRepositoryRootPath());
        settings.setRemoteGitRepositoryURL(pluginSettingsDialog.getRemoteGitRepositoryURL());
        saveAllSettings();
        changeListener.onSettingsChange(settings, oldSettings);
    }

    public void setSettingsToDialog(PluginSettingsDialog pluginSettingsDialog) {
        pluginSettingsDialog.setPluginActive(settings.isPluginActive());
        pluginSettingsDialog.setCommonTags(settings.getCommonTags());
        pluginSettingsDialog.setFeaturePath(settings.getFeaturePath());
        pluginSettingsDialog.setGitRepositoryRootPath(settings.getGitRepositoryRootPath());
        pluginSettingsDialog.setRemoteGitRepositoryURL(settings.getRemoteGitRepositoryURL());
    }

    public void setSettingsToNewFeatureDialog(NewFeatureDialog newFeatureDialog) {
        newFeatureDialog.setCommonTags(getCommonTags());
    }

    public boolean isPluginActive() {
        return settings.isPluginActive();
    }

    public EditableCommonTagList getCommonTags() {
        return settings.getCommonTags();
    }

    public String getFeaturePath() {
        return settings.getFeaturePath();
    }

    public String getGitRepositoryRootPath() {
        return settings.getGitRepositoryRootPath();
    }

    public String getRemoteGitRepositoryURL() {
        return settings.getRemoteGitRepositoryURL();
    }

    public void setRemoteGitRepositoryURL(String remoteGitRepositoryURL) {
        settings.setRemoteGitRepositoryURL(remoteGitRepositoryURL);
    }
}
