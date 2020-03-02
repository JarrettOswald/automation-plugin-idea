package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.CommonTag;

import java.util.List;

public class FixedCommonTagTableModel extends AbstractTagTableModel {
    private static CommonTagCellRenderer tagCellRenderer = new CommonTagCellRenderer();

    private List<CommonTag> allTags;

    public FixedCommonTagTableModel(List<CommonTag> commonTags) {
        this.allTags = commonTags;
    }

    @Override
    public AbstractTagCellRenderer getCellRenderer() {
        return tagCellRenderer;
    }

    private static class CommonTagCellRenderer extends AbstractTagTableModel.AbstractTagCellRenderer {
        boolean isIconVisibleFor(AbstractTag tag) {
            return false;
        }
    }
}
