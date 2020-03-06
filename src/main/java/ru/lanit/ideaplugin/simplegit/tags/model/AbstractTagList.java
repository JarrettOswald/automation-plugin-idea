package ru.lanit.ideaplugin.simplegit.tags.model;

import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

abstract public class AbstractTagList<T extends AbstractTag> extends AbstractTableModel {
    protected ArrayList<T> tags;
    protected JTable table;
    protected FocusListener focusListener;

    public AbstractTagList() {
        tags = new ArrayList<>();
    }

    public AbstractTagList(List<T> tags) {
        this.tags = new ArrayList<>(tags);
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
    public Class<?> getColumnClass(int column) {
        return AbstractTag.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (row < tags.size() && row >= 0) {
            return getTag(row);
        }
        return null;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        T tag = getTag(row);
        String name = (String) value;
        if (tag.getName() == null && (name == null || name.isEmpty())) {
            removeTag(row);
        } else {
            tag.setName(name);
            fireTableCellUpdated(row, column);
        }
    }

    @Override
    abstract public boolean isCellEditable(int rowIndex, int columnIndex);

    private T getTag(int row) {
        return tags.get(row);
    }

    public void clear() {
        int size = tags.size() - 1;
        if (size >= 0) {
            tags.clear();
            fireTableRowsDeleted(0, size);
        }
    }

    public void insertTag(int row, T tag) {
        tags.add(row, tag);
        fireTableRowsInserted(row, row);
    }

    public List<T> removeTags(int[] rows) {
        return Arrays.stream(rows)
                .map(table::convertRowIndexToModel)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .map(this::removeTag)
                .collect(Collectors.toList());
    }

    public T removeTag(int row) {
        T tag = tags.remove(row);
        fireTableRowsDeleted(row, row);
        return tag;
    }

    abstract public AbstractTagCellRenderer<T> getCellRenderer();

    public void attachToTable(JTable table) {
        this.table = table;
        table.setAutoCreateColumnsFromModel(true);
        table.setTableHeader(null);
        table.setShowGrid(false);
        table.setModel(this);
        table.setDefaultRenderer(AbstractTag.class, getCellRenderer());
        table.setDefaultEditor(Object.class, new TagCellEditor());
        TableRowSorter<AbstractTagList<T>> sorter = new TableRowSorter<>(this);
        table.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = Collections.singletonList(
                new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        this.focusListener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {}

            @Override
            public void focusLost(FocusEvent e) {
                for (int i = tags.size() - 1; i >= 0; i--) {
                    if (tags.get(i).isUnnamed()) removeTag(i);
                }
            }
        };
    };

    public Integer getTagIndex(T tag) {
        return tags.indexOf(tag);
    }

    public ArrayList<T> getTags() {
        return new ArrayList<>(tags);
    }

    public <E extends T> void addTags(@NotNull List<E> newTags) {
        if (newTags.size() > 0) {
            int from = tags.size();
            tags.addAll(newTags);
            fireTableRowsInserted(from, tags.size() - 1);
        }
    }

    protected abstract <E extends T> E getNewTag();

    public <E extends T> void addNewTag() {
        E newTag = getNewTag();
        int row = getRowCount();
        insertTag(row, newTag);
        table.changeSelection(row, 0, false, false);
        table.editCellAt(row, 0);
        Component editor = table.getEditorComponent();
        editor.addFocusListener(focusListener);
        editor.requestFocus();
    }

    abstract static class AbstractTagCellRenderer<T extends AbstractTag> extends JLabel implements TableCellRenderer {
        abstract boolean isIconVisibleFor(T tag);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int index, int column) {
            T tag = (T) value;
            if (tag != null) {
                String name = tag.getName();
                setText(name);
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                    setForeground(table.getSelectionForeground());
                } else {
                    setBackground(table.getBackground());
                    setForeground(table.getForeground());
                }
                if (isIconVisibleFor(tag)) {
                    setIcon(tag.getIcon());
                } else {
                    setIcon(null);
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
            return null;
        }
    }

    private static class TagCellEditor extends DefaultCellEditor {

        Object value;

        public TagCellEditor() {
            super(new JTextField());
            JTextField component = (JTextField) getComponent();
            component.setName("Table.editor");
            NoSpacesDocument nsd = new NoSpacesDocument();
            nsd.setNoSpaces(true);
            component.setDocument(nsd);
        }

        public boolean stopCellEditing() {
            String s = (String) super.getCellEditorValue();
            try {
                value = s;
                return super.stopCellEditing();
            }
            catch (Exception e) {
                ((JComponent)getComponent()).setBorder(new LineBorder(Color.red));
                return false;
            }
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected,
                                                     int row, int column) {
            this.value = null;
            ((JComponent)getComponent()).setBorder(new LineBorder(Color.black));
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        public Object getCellEditorValue() {
            return value;
        }
    }

    private static class NoSpacesDocument extends PlainDocument {
        private boolean noSpaces = true;

        public void setNoSpaces(boolean flag) {
            noSpaces = flag;
        }

        public void insertString(int offset, String str, AttributeSet attSet)
                throws BadLocationException {
            if (noSpaces)
                str = str.replaceAll("\\s", "");
            super.insertString(offset, str, attSet);
        }

    }
}
