package ru.lanit.ideaplugin.simplegit.tags.tag;

import com.intellij.icons.AllIcons;
import ru.lanit.ideaplugin.simplegit.tags.model.AbstractTagList;

import javax.swing.*;

public class FavoriteTag extends AbstractTag {
    private static Icon ICON = initializeIcon(AllIcons.Toolwindows.ToolWindowFavorites);

    public FavoriteTag(AbstractTagList<FavoriteTag> tagList) {
        super(tagList);
    }

    public FavoriteTag(AbstractTagList<FavoriteTag> tagList, String name) {
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
