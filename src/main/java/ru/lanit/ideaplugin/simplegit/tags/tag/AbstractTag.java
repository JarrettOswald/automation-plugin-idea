package ru.lanit.ideaplugin.simplegit.tags.tag;

import com.intellij.ui.SizedIcon;
import com.intellij.util.ui.JBUI;

import javax.swing.*;

public abstract class AbstractTag {
    private String name;
    private Integer index;

    public AbstractTag() {}

    public AbstractTag(String name, Integer index) {
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    abstract public boolean isCommon();

    abstract public boolean isEnabled();

    abstract public Icon getIcon();

    protected static Icon initializeIcon(Icon icon) {
        return JBUI.scale(new SizedIcon(icon, 16, 16));
    }

    @Override
    public String toString() {
        return name;
    }
}
