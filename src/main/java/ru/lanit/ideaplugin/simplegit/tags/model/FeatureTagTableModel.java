package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.FeatureTag;

public class FeatureTagTableModel extends AbstractTagTableModel {
    private static FeatureTagCellRenderer tagCellRenderer = new FeatureTagCellRenderer();

    @Override
    public AbstractTagCellRenderer getCellRenderer() {
        return tagCellRenderer;
    }

    private static class FeatureTagCellRenderer extends AbstractTagTableModel.AbstractTagCellRenderer {
        boolean isIconVisibleFor(AbstractTag tag) {
            return !(tag instanceof FeatureTag);
        }
    }
}
