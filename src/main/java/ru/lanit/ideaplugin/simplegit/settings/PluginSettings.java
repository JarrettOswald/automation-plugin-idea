package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertyName;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NonNls;

import java.util.Locale;

public class PluginSettings {
    private static final Logger log = Logger.getInstance(PluginSettings.class);
    @NonNls public static final String SIMPLEGIT_SETTINGS_LOCALE = "simplegit.settings.locale";

    @PropertyName(value = SIMPLEGIT_SETTINGS_LOCALE)
    private String locale;

    public Locale getLocale() {
        return locale == null || locale.isEmpty() ? Locale.ENGLISH: Locale.forLanguageTag(locale);
    }
}
