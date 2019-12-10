package ru.lanit.ideaplugin.simplegit.dialogs;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import javax.swing.*;

public class PluginOptionsDialogGUI extends JDialog {
    protected JPanel contentPane;
    protected JCheckBox isPluginActive;
    protected JTextField commonTags;
    protected TextFieldWithBrowseButton featureCatalog;

    public PluginOptionsDialogGUI() {
        setContentPane(contentPane);
        setModal(true);
        pack();
        validate();
    }
}
