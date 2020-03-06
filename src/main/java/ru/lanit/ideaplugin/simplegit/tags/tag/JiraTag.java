package ru.lanit.ideaplugin.simplegit.tags.tag;

import com.intellij.icons.AllIcons;
import ru.lanit.ideaplugin.simplegit.tags.model.AbstractTagList;

import javax.swing.*;

public class JiraTag extends AbstractTag {
    private static Icon ICON = initializeIcon(AllIcons.RunConfigurations.Remote);

    public JiraTag(AbstractTagList<AbstractTag> tagList) {
        super(tagList);
    }

    public JiraTag(AbstractTagList<AbstractTag> tagList, String name) {
        super(tagList, name);
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
