package ru.lanit.ideaplugin.simplegit.tags.tag;

import com.intellij.ui.SizedIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.tags.model.AbstractTagList;

import javax.swing.*;

public abstract class AbstractTag<T extends AbstractTagList<AbstractTag<T>>> implements Comparable<AbstractTag<T>> {
    private final T tagList;
    private String name;

    public AbstractTag(T tagList) {
        this.tagList = tagList;
    }

    public AbstractTag(T tagList, String name) {
        this(tagList);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getTagAsString() {
        if (name == null) return "";
        return "@" + name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getIndex() {
        return tagList.getTagIndex(this);
    }

    public boolean isUnnamed() {
        return this.name == null;
    }

    abstract public boolean isEnabled();

    abstract public Icon getIcon();

    protected static Icon initializeIcon(Icon icon) {
        return JBUI.scale(new SizedIcon(icon, 16, 16));
    }

    @Override
    public int compareTo(@NotNull AbstractTag tag) {
        Integer typeOrdinal1 = TagType.getTagTypeByTag(this).ordinal();
        Integer typeOrdinal2 = TagType.getTagTypeByTag(tag).ordinal();
        int result = typeOrdinal1.compareTo(typeOrdinal2);
        if (result == 0) {
            return getIndex().compareTo(tag.getIndex());
        }
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}
