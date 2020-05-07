package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import ru.lanit.ideaplugin.simplegit.dialogs.newfeature.NewFeatureDialog;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;
import ru.lanit.ideaplugin.simplegit.tags.model.EditableFavoriteTagList;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class SettingsProvider {
    private static final Logger log = Logger.getInstance(SettingsProvider.class);
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private final PropertiesComponent projectProperties;
    private final PropertiesComponent pluginProperties;
    private ProjectSettings projectSettings;
    private PluginSettings pluginSettings;

    public SettingsProvider(Project project) {
        pluginProperties = PropertiesComponent.getInstance();
        projectProperties = PropertiesComponent.getInstance(project);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        changeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        changeSupport.removePropertyChangeListener(l);
    }

    private void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    private void firePropertyChange(String propertyName, String oldValue, String newValue) {
        if (newValue.equals(oldValue)) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public boolean restoreAllSettings() {
        ProjectSettings oldProjectSettings = projectSettings;
        PluginSettings oldPluginSettings = pluginSettings;
        projectSettings = new ProjectSettings();
        pluginSettings = new PluginSettings();
        if (projectProperties.loadFields(projectSettings)) {
            projectSettings.cleanup();
            if (pluginProperties.loadFields(pluginSettings)) {
                checkAndFirePropertyChange(oldPluginSettings, pluginSettings);
                return true;
            }
        }
        projectSettings = oldProjectSettings;
        pluginSettings = oldPluginSettings;
        return false;
    }

    private void checkAndFirePropertyChange(PluginSettings oldPluginSettings, PluginSettings pluginSettings) {
    }

    private boolean saveAllSettings() {
        boolean result = projectProperties.saveFields(projectSettings);
        return pluginProperties.saveFields(pluginSettings) && result;
    }

    public void setSettingsFromDialog(PluginSettingsDialog pluginSettingsDialog) {
        ProjectSettings oldSettings = projectSettings;
        projectSettings = new ProjectSettings();
        projectSettings.setPluginActive(pluginSettingsDialog.isPluginActive());
        projectSettings.setFavoriteTags(pluginSettingsDialog.getFavoriteTags());
        projectSettings.setFeaturePath(pluginSettingsDialog.getFeaturePath());
        projectSettings.setGitRepositoryRootPath(pluginSettingsDialog.getGitRepositoryRootPath());
        projectSettings.setRemoteGitRepositoryURL(pluginSettingsDialog.getRemoteGitRepositoryURL());
        projectSettings.setRemoteMainBranch(pluginSettingsDialog.getRemoteMainBranch());
        saveAllSettings();
    }

    public void setSettingsToDialog(PluginSettingsDialog pluginSettingsDialog) {
        pluginSettingsDialog.setPluginActive(projectSettings.isPluginActive());
        pluginSettingsDialog.setFavoriteTags(projectSettings.getFavoriteTags());
        pluginSettingsDialog.setFeaturePath(projectSettings.getFeaturePath());
        pluginSettingsDialog.setGitRepositoryRootPath(projectSettings.getGitRepositoryRootPath());
        pluginSettingsDialog.setRemoteGitRepositoryURL(projectSettings.getRemoteGitRepositoryURL());
        pluginSettingsDialog.setRemoteMainBranch(projectSettings.getRemoteMainBranch());
    }

    public void setSettingsToNewFeatureDialog(NewFeatureDialog newFeatureDialog) {
        newFeatureDialog.setFavoriteTags(getFavoriteTags());
    }

    public boolean isPluginActive() {
        return projectSettings.isPluginActive();
    }

    public EditableFavoriteTagList getFavoriteTags() {
        return projectSettings.getFavoriteTags();
    }

    public String getFeaturePath() {
        return projectSettings.getFeaturePath();
    }

    public String getGitRepositoryRootPath() {
        return projectSettings.getGitRepositoryRootPath();
    }

    public String getRemoteGitRepositoryURL() {
        return projectSettings.getRemoteGitRepositoryURL();
    }

    public void setRemoteGitRepositoryURL(String remoteGitRepositoryURL) {
        projectSettings.setRemoteGitRepositoryURL(remoteGitRepositoryURL);
    }
}
