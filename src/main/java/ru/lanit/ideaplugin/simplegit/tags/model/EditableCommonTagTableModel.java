package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;

public class EditableCommonTagTableModel extends AbstractTagTableModel {
    private static CommonTagCellRenderer tagCellRenderer = new CommonTagCellRenderer();

    @Override
    public AbstractTagCellRenderer getCellRenderer() {
        return tagCellRenderer;
    }

    private static class CommonTagCellRenderer extends AbstractTagCellRenderer {
        boolean isIconVisibleFor(AbstractTag tag) {
            return false;
        }
    }
}
