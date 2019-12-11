package ru.lanit.ideaplugin.simplegit.dialogs.pluginoptions;

import com.intellij.ide.actions.OpenProjectFileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;

public class PluginOptionsDialog extends DialogWrapper {
    private SimpleGitPlugin plugin;
    private PluginOptionsDialogGUI dialogGUI;

    public PluginOptionsDialog(@Nullable Project project) {
        super(project);
        this.plugin = SimpleGitPlugin.getPluginFor(project);
        init();
        setTitle("SimpleGit project options");
        setResizable(false);
    }

    boolean isOkEnabled() {
        // return true if dialog can be closed
        return true;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        dialogGUI = new PluginOptionsDialogGUI();

        FileChooserDescriptor descriptor = new OpenProjectFileChooserDescriptor(false, false);
        dialogGUI.featureCatalog.addBrowseFolderListener(new TextBrowseFolderListener(descriptor) {
            @Override
            public void actionPerformed(ActionEvent e) {
                descriptor.setRoots(plugin.getProject().getBaseDir());
                String current = dialogGUI.featureCatalog.getText();
                FileChooserDialog fc = FileChooserFactory.getInstance().createFileChooser(descriptor, plugin.getProject(), null);

                VirtualFile[] selection;
                if (!current.isEmpty()) {
                    selection = fc.choose(plugin.getProject(), plugin.getProject().getBaseDir().findFileByRelativePath(current));
                } else {
                    selection = fc.choose(plugin.getProject());
                }

                dialogGUI.featureCatalog.setText(VfsUtil.getRelativePath(selection[0], plugin.getProject().getBaseDir()));
            }

        });

        dialogGUI.isPluginActive.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                boolean enabled = dialogGUI.isPluginActive.isSelected();
                dialogGUI.featureCatalog.setEnabled(enabled);
                dialogGUI.commonTags.setEnabled(enabled);
            }
        });

        return dialogGUI.getRootPane();
    }

    @Override
    public ValidationInfo doValidate() {
        if (dialogGUI.isPluginActive.isSelected() && dialogGUI.featureCatalog.getText().isEmpty()) {
            return new ValidationInfo("Need to select features catalog", dialogGUI.featureCatalog);
        }
        return null;
    }

    public String getFeatureCatalog() {
        return dialogGUI.featureCatalog.getText();
    }
}