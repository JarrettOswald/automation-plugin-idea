package ru.lanit.ideaplugin.simplegit.tags.tag;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.tags.model.AbstractTagList;

import javax.swing.*;

public class CommonTag extends AbstractTag {
    private static Icon ICON = initializeIcon(AllIcons.Toolwindows.ToolWindowFavorites);

    public CommonTag(AbstractTagList<CommonTag> tagList) {
        super(tagList);
    }

    public CommonTag(AbstractTagList<CommonTag> tagList, String name) {
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
