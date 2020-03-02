package ru.lanit.ideaplugin.simplegit.tags.tag;

import com.intellij.icons.AllIcons;

import javax.swing.*;

public class CommonTag extends AbstractTag {
    private static Icon ICON = initializeIcon(AllIcons.Toolwindows.ToolWindowFavorites);

    public CommonTag(String name) {
        super(name, null);
    }

    public CommonTag(String name, Integer index) {
        super(name, index);
    }

    @Override
    public boolean isCommon() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }
}
