package ru.lanit.ideaplugin.simplegit;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SizedIcon;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class FeatureTag {
    private static TagCellRenderer tagCellRenderer = new TagCellRenderer();

    private String name;
    private final boolean isCommon;
    private final Integer index;
    private final boolean isEnabled;

    public FeatureTag(String name, boolean isEnabled) {
        this.name = name;
        this.isCommon = false;
        this.isEnabled = isEnabled;
        this.index = null;
    }

    public FeatureTag(String name, int index) {
        this.name = name;
        this.isCommon = true;
        this.isEnabled = true;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public Integer getIndex() {
        return index;
    }

    public boolean isCommon() {
        return isCommon;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public String toString() {
        return name;
    }

    public static TagCellRenderer getCellRenderer() {
        return tagCellRenderer;
    }

    public boolean isEditable() {
        return true;
    }

    public void setName(String name) {
        this.name = name;
    }

    private static class TagCellRenderer extends JLabel implements ListCellRenderer<Object> {
        final static Icon commonIcon = JBUI.scale(new SizedIcon(AllIcons.Toolwindows.ToolWindowFavorites, 16, 16));
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            FeatureTag tag = (FeatureTag) value;
            setText(tag.getName());
            if (tag.isCommon()) {
                setIcon(commonIcon);
            }
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled() && tag.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }
   }
}
