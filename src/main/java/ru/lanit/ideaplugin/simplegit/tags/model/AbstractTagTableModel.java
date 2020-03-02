package ru.lanit.ideaplugin.simplegit.tags.model;

import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

abstract public class AbstractTagTableModel extends AbstractTableModel {
    private List<AbstractTag> tags;

    public AbstractTagTableModel() {
        tags = new ArrayList<>();
    }

    public AbstractTagTableModel(List<AbstractTag> tags) {
        super();
        this.tags = tags;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int column) {
        return "Tag";
    }

    @Override
    public int getRowCount() {
        return tags.size();
    }

    @Override
    public Class getColumnClass(int column) {
        return AbstractTag.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return getTag(row).getName();
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        getTag(row).setName((String) value);
        fireTableCellUpdated(row, column);
    }

    private AbstractTag getTag(int row) {
        return tags.get(row);
    }

    public void addTag(AbstractTag tag) {
        insertTag(getRowCount(), tag);
    }

    public void insertTag(int row, AbstractTag tag) {
        tags.add(row, tag);
        fireTableRowsInserted(row, row);
    }

    public void clear() {
        int size = tags.size() - 1;
        if (size >= 0) {
            tags.clear();
            fireTableRowsDeleted(0, size);
        }
    }

    public void removeTag(int row) {
        tags.remove(row);
        fireTableRowsDeleted(row, row);
    }

    abstract public AbstractTagCellRenderer getCellRenderer();

    public void attachToTable(JTable table) {
        table.setModel(this);
        table.setDefaultRenderer(AbstractTag.class, getCellRenderer());
    };

    abstract static class AbstractTagCellRenderer extends JLabel implements TableCellRenderer {
        abstract boolean isIconVisibleFor(AbstractTag tag);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int index, int column) {
            AbstractTag tag = (AbstractTag) value;
            setText(tag.getName());
            if (isIconVisibleFor(tag)) {
                setIcon(tag.getIcon());
            }
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            setEnabled(table.isEnabled() && tag.isEnabled());
            setFont(table.getFont());
            setOpaque(true);
            return this;
        }
    }
}
