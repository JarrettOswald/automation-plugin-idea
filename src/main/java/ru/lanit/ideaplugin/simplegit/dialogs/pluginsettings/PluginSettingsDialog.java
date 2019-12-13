package ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings;

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
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class PluginSettingsDialog extends DialogWrapper {
    private SimpleGitProjectComponent plugin;

    private JPanel contentPane;
    private JCheckBox isPluginActive;
    private JTextField commonTags;
    private TextFieldWithBrowseButton featurePath;

    public PluginSettingsDialog(@Nullable Project project) {
        super(project);
        this.plugin = project.getComponent(SimpleGitProjectComponent.class);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.fill = 1;

        featurePath = new TextFieldWithBrowseButton();
        featurePath.setEnabled(false);
        featurePath.setEditable(false);
        contentPane.add(featurePath, constraints);

        setModal(true);
        pack();
        validate();

        init();
        setTitle("SimpleGit project settings");
        setResizable(false);
    }

    boolean isOkEnabled() {
        // return true if dialog can be closed
        return true;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        FileChooserDescriptor descriptor = new OpenProjectFileChooserDescriptor(false, false);
        featurePath.addBrowseFolderListener(new TextBrowseFolderListener(descriptor) {
            @Override
            public void actionPerformed(ActionEvent e) {
                descriptor.setRoots(plugin.getProject().getBaseDir());
                String current = featurePath.getText();
                FileChooserDialog fc = FileChooserFactory.getInstance().createFileChooser(descriptor, plugin.getProject(), null);

                VirtualFile[] selection;
                if (!current.isEmpty()) {
                    selection = fc.choose(plugin.getProject(), plugin.getProject().getBaseDir().findFileByRelativePath(current));
                } else {
                    selection = fc.choose(plugin.getProject());
                }
                if (selection.length > 0) {
                    featurePath.setText(VfsUtil.getRelativePath(selection[0], plugin.getProject().getBaseDir()));
                }
            }

        });

        isPluginActive.addChangeListener(e -> {
            boolean enabled = isPluginActive.isSelected();
            featurePath.setEnabled(enabled);
            commonTags.setEnabled(enabled);
        });

        return contentPane;
    }

    @Override
    public ValidationInfo doValidate() {
        if (isPluginActive.isSelected() && featurePath.getText().isEmpty()) {
            return new ValidationInfo("Need to select features path", featurePath);
        }
        return null;
    }

    public String getFeaturePath() {
        return featurePath.getText();
    }

    public void setFeaturePath(String path) {
        featurePath.setText(path);
    }

    public String getCommonTags() {
        return commonTags.getText();
    }

    public void setCommonTags(String tags) {
        commonTags.setText(tags);
    }

    public boolean isPluginActive() {
        return isPluginActive.isSelected();
    }

    public void setPluginActive(boolean active) {
        isPluginActive.setSelected(active);
    }
}