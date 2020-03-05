package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.CommonTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EditableCommonTagList extends AbstractTagList<CommonTag> {
    private static CommonTagCellRenderer tagCellRenderer = new CommonTagCellRenderer();

    public EditableCommonTagList() {
        super();
    }

    public EditableCommonTagList(String[] tagNames) {
        tags = Arrays.stream(tagNames)
                .map(name -> new CommonTag(this, name))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public AbstractTagCellRenderer<CommonTag> getCellRenderer() {
        return tagCellRenderer;
    }

    @Override
    protected CommonTag getNewTag() {
        return new CommonTag(this);
    }

    private static class CommonTagCellRenderer extends AbstractTagCellRenderer<CommonTag> {
        boolean isIconVisibleFor(CommonTag tag) {
            return false;
        }
    }

    @Override
    public String toString() {
        return tags.stream().map(CommonTag::toString).collect(Collectors.joining(";"));
    }
}
