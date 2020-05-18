package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.FavoriteTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EditableFavoriteTagList extends AbstractTagList<FavoriteTag> {
    private static FavoriteTagCellRenderer tagCellRenderer = new FavoriteTagCellRenderer();

    public EditableFavoriteTagList() {
        super();
    }

    public EditableFavoriteTagList(String[] tagNames) {
        tags = Arrays.stream(tagNames)
                .map(name -> new FavoriteTag(this, name))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public AbstractTagCellRenderer<FavoriteTag> getCellRenderer() {
        return tagCellRenderer;
    }

    @Override
    protected FavoriteTag getNewTag() {
        return new FavoriteTag(this);
    }

    private static class FavoriteTagCellRenderer extends AbstractTagCellRenderer<FavoriteTag> {
        boolean isIconVisibleFor(FavoriteTag tag) {
            return false;
        }
    }

    @Override
    public String toString() {
        return tags.stream().map(FavoriteTag::toString).collect(Collectors.joining(";"));
    }
}
