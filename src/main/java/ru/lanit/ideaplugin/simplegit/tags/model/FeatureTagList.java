package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.FeatureTag;

import java.awt.*;

public class FeatureTagList extends AbstractTagList<AbstractTag> {
    private static FeatureTagCellRenderer tagCellRenderer = new FeatureTagCellRenderer();

    @Override
    public AbstractTagCellRenderer<AbstractTag> getCellRenderer() {
        return tagCellRenderer;
    }

    @Override
    protected FeatureTag getNewTag() {
        return new FeatureTag(this);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        AbstractTag tag = (AbstractTag) getValueAt(rowIndex, columnIndex);
        return tag instanceof FeatureTag;
    }

    private static class FeatureTagCellRenderer extends AbstractTagCellRenderer<AbstractTag> {
        boolean isIconVisibleFor(AbstractTag tag) {
            return !(tag instanceof FeatureTag);
        }
    }
}
