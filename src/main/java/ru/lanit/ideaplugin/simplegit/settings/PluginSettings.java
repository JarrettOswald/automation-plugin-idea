package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertyName;

public class PluginSettings {
    @PropertyName(value = "ru.lanit.ideaplugin.simplegit.settings.isPluginActive", defaultValue = "false")
    boolean isPluginActive;
    @PropertyName(value = "ru.lanit.ideaplugin.simplegit.settings.commonTags")
    String[] commonTags;
    @PropertyName(value = "ru.lanit.ideaplugin.simplegit.settings.featureCatalog")
    String featureCatalog;

    public boolean isPluginActive() {
        return isPluginActive;
    }

    public String[] getCommonTags() {
        return commonTags;
    }

    public String getFeatureCatalog() {
        return featureCatalog;
    }

}
