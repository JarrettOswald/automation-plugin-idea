package ru.lanit.ideaplugin.simplegit.tags.tag;

import javax.swing.*;

public class FeatureTag extends AbstractTag {
    public FeatureTag(String name) {
        super(name, null);
    }

    public FeatureTag(String name, Integer index) {
        super(name, index);
    }

    @Override
    public boolean isCommon() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Icon getIcon() {
        return null;
    }
}
