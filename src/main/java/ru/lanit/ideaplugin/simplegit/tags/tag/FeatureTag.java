package ru.lanit.ideaplugin.simplegit.tags.tag;

import ru.lanit.ideaplugin.simplegit.tags.model.AbstractTagList;
import ru.lanit.ideaplugin.simplegit.tags.model.FeatureTagList;

import javax.swing.*;

public class FeatureTag extends AbstractTag {
    public FeatureTag(AbstractTagList<AbstractTag> tagList) {
        super(tagList);
    }

    public FeatureTag(AbstractTagList<AbstractTag> tagList, String name) {
        super(tagList, name);
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
