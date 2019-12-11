package ru.lanit.ideaplugin.simplegit.settings;

public interface SettingsChangeListener {
    void onSettingsChange(PluginSettings newOptions, PluginSettings oldOptions);
}
