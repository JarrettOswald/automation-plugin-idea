package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.CommonTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class FixedCommonTagList extends AbstractTagList<CommonTag> {
    private static CommonTagCellRenderer tagCellRenderer = new CommonTagCellRenderer();

    private ArrayList<CommonTag> allTags;

    public FixedCommonTagList() {
        super();
        this.allTags = new ArrayList<>();
    }

    public FixedCommonTagList(FixedCommonTagList commonTagsList) {
        super();
        this.allTags = commonTagsList.getTags();
        this.tags.addAll(allTags);
    }

    public FixedCommonTagList(String[] tagNames) {
        super();
        allTags = Arrays.stream(tagNames)
                .map(name -> new CommonTag(this, name))
                .collect(Collectors.toCollection(ArrayList::new));
        this.tags.addAll(allTags);
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
