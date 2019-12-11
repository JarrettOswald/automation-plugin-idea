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

    public boolean isPluginActive() {
        return pluginActive;
    }

    public List<String> getCommonTags() {
        return commonTags;
    }

    public String getFeaturePath() {
        return featurePath == null ? "" : featurePath;
    }
}
