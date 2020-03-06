package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.CommonTag;

public class FixedCommonTagList extends AbstractTagList<CommonTag> {
    private static CommonTagCellRenderer tagCellRenderer = new CommonTagCellRenderer();

    public FixedCommonTagList() {
        super();
    }

    public FixedCommonTagList(EditableCommonTagList commonTagsList) {
        this.tags = commonTagsList.getTags();
    }

    @Override
    public AbstractTagCellRenderer<CommonTag> getCellRenderer() {
        return tagCellRenderer;
    }

    @Override
    protected CommonTag getNewTag() {
        return new CommonTag(this);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    private static class CommonTagCellRenderer extends AbstractTagCellRenderer<CommonTag> {
        boolean isIconVisibleFor(CommonTag tag) {
            return false;
        }
    }
}
