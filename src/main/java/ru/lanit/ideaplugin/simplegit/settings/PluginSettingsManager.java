package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;

public class PluginSettingsManager {
    private final PropertiesComponent properties;
    private final SettingsChangeListener changeListener;
    private PluginSettings settings;

    public PluginSettingsManager(Project project, SettingsChangeListener changeListener) {
        properties = PropertiesComponent.getInstance(project);
        this.changeListener = changeListener;
    }

    public boolean restoreAllSettings() {
        PluginSettings oldSettings = settings;
        settings = new PluginSettings();
        if (properties.loadFields(settings)) {
            changeListener.onSettingsChange(settings, oldSettings);
            return true;
        }
        settings = oldSettings;
        return false;
    }

    public boolean saveAllSettings() {
        return properties.saveFields(settings);
    }

    public void setSettingsFromDialog(PluginSettingsDialog pluginSettingsDialog) {
        PluginSettings oldSettings = settings;
        settings = new PluginSettings();
        settings.isPluginActive = pluginSettingsDialog.isPluginActive();
        if (settings.isPluginActive) {
            settings.commonTags = pluginSettingsDialog.getCommonTags().isEmpty()
                    ? null
                    : pluginSettingsDialog.getCommonTags().split(";");
            settings.featureCatalog = pluginSettingsDialog.getFeatureCatalog();
        }
        saveAllSettings();
        changeListener.onSettingsChange(settings, oldSettings);
    }

    public void setSettingsToDialog(PluginSettingsDialog pluginSettingsDialog) {
        pluginSettingsDialog.setPluginActive(settings.isPluginActive);
        pluginSettingsDialog.setCommonTags(settings.commonTags == null
                ? ""
                : String.join(";", settings.commonTags)
        );
        pluginSettingsDialog.setFeatureCatalog(settings.featureCatalog);
    }

    public boolean isPluginActive() {
        return settings.isPluginActive;
    }

    public String[] getCommonTags() {
        return settings.commonTags;
    }

    public String getFeatureCatalog() {
        return settings.featureCatalog;
    }
}
