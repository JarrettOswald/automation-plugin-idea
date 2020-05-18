package ru.lanit.ideaplugin.simplegit.localization;

import org.jetbrains.annotations.NonNls;

import java.util.ResourceBundle;

public class Language {
    @NonNls
    public static ResourceBundle simpleGitPluginBundle = ResourceBundle.getBundle("i18n.simpleGitPluginBundle", new UTF8Control());
}
