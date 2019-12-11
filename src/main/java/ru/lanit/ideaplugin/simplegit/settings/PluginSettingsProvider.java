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
    }

    private boolean isNull(String value) {
        return (value == null || value.equals("null"));
    }

    public void setSettingsFromDialog(PluginSettingsDialog pluginSettingsDialog) {
        PluginSettings oldSettings = settings;
        settings = new PluginSettings();
        settings.pluginActive = pluginSettingsDialog.isPluginActive();
        if (settings.pluginActive) {
            settings.commonTags = pluginSettingsDialog.getCommonTags().isEmpty()
                    ? null
                    : Arrays.asList(pluginSettingsDialog.getCommonTags().split(";"));
            settings.featurePath = pluginSettingsDialog.getFeaturePath();
        }
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
}
