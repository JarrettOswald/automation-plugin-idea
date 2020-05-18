package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.FavoriteTag;

public class FixedFavoriteTagList extends AbstractTagList<FavoriteTag> {
    private static FavoriteTagCellRenderer tagCellRenderer = new FavoriteTagCellRenderer();

    public FixedFavoriteTagList() {
        super();
    }

    public FixedFavoriteTagList(EditableFavoriteTagList favoriteTagsList) {
        this.tags = favoriteTagsList.getTags();
    }

    @Override
    public AbstractTagCellRenderer<FavoriteTag> getCellRenderer() {
        return tagCellRenderer;
    }

    @Override
    protected FavoriteTag getNewTag() {
        return new FavoriteTag(this);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    private static class FavoriteTagCellRenderer extends AbstractTagCellRenderer<FavoriteTag> {
        boolean isIconVisibleFor(FavoriteTag tag) {
            return false;
        }
    }
}
