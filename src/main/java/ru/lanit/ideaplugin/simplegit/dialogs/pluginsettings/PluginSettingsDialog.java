package ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings;

import com.intellij.ide.actions.OpenProjectFileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PluginSettingsDialog extends DialogWrapper {
    private SimpleGitProjectComponent plugin;

    private JPanel contentPane;
    private JCheckBox isPluginActive;
    private JTextField commonTags;
    private JComboBox<String> gitRepositoryRootPath;
    private JComboBox<String> remoteGitRepositoryURL;
    private TextFieldWithBrowseButton featurePath;

    public PluginSettingsDialog(@Nullable Project project) {
        super(project);
        this.plugin = project.getComponent(SimpleGitProjectComponent.class);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weightx = 1;
        constraints.fill = 1;
        constraints.insets = JBUI.insets(2, 0);

        featurePath = new TextFieldWithBrowseButton();
        featurePath.setEnabled(false);
        featurePath.setEditable(false);
        contentPane.add(featurePath, constraints);

        isPluginActive.setEnabled(false);

        fillGitRepositoryRootPath();
        setModal(true);
        pack();

        init();
        setTitle("SimpleGit project settings");
        setResizable(false);

        initValidation();
    }

    private void fillGitRepositoryRootPath() {
        List<GitRepository> repositories = plugin.getGitManager().getGitRepositories();
        for (GitRepository repositoriy : repositories) {
            String path = repositoriy.getRoot().getPath();
            this.gitRepositoryRootPath.addItem(path);
        }
        if (this.gitRepositoryRootPath.getItemCount() == 1) {
            this.gitRepositoryRootPath.setSelectedIndex(0);
        }
    }

    private boolean isGitRepositoryRootPathSelectable() {
        return this.gitRepositoryRootPath.getItemCount() >= 2;
    }

    private void fillRemoteGitRepositoryURL(PropertyChangeEvent propertyChangeEvent) {
        String repositoryPath = getGitRepositoryRootPath();
        remoteGitRepositoryURL.removeAllItems();
        if (repositoryPath != null) {
            Optional<GitRepository> repository = plugin.getGitManager().getGitRepositories().stream()
                    .filter(repo -> repo.getRoot().getPath().equals(repositoryPath))
                    .findFirst();
            if (repository.isPresent()) {
                Collection<GitRemote> repositories = plugin.getGitManager().getRemoteGitRepositories(repository.get());
                for (GitRemote repositoriy : repositories) {
                    String url = repositoriy.getFirstUrl();
                    this.remoteGitRepositoryURL.addItem(url);
                }
                if (this.remoteGitRepositoryURL.getItemCount() >= 2) {
                    this.remoteGitRepositoryURL.setEnabled(isPluginActive.isSelected());
                    return;
                }
                if (this.remoteGitRepositoryURL.getItemCount() == 1) {
                    this.remoteGitRepositoryURL.setSelectedIndex(0);
                }
            }
        }
        this.remoteGitRepositoryURL.setEnabled(false);
        if (isShowing()) {
            initValidation();
        }
    }

    private boolean isRemoteGitRepositoryURLSelectable() {
        return this.gitRepositoryRootPath.getItemCount() >= 2;
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

        isPluginActive.addChangeListener(this::updateEnabledStateOfElements);
        updateEnabledStateOfElements(null);
        gitRepositoryRootPath.addPropertyChangeListener(this::fillRemoteGitRepositoryURL);
        return contentPane;
    }

    private void updateEnabledStateOfElements(ChangeEvent changeEvent) {
        boolean enabled = isPluginActive.isSelected();
        featurePath.setEnabled(enabled);
        commonTags.setEnabled(enabled);
        gitRepositoryRootPath.setEnabled(enabled && isGitRepositoryRootPathSelectable());
    }

    @Override
    public ValidationInfo doValidate() {
        if (gitRepositoryRootPath.getItemCount() == 0 || remoteGitRepositoryURL.getItemCount() == 0) {
            isPluginActive.setEnabled(false);
            if (gitRepositoryRootPath.getItemCount() == 0)
                return new ValidationInfo("Need to set Git Repository", isPluginActive);
            return new ValidationInfo("Need to set Remote Git Repository", isPluginActive);
        }
        isPluginActive.setEnabled(true);
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

    public String getGitRepositoryRootPath() {
        return (String) gitRepositoryRootPath.getSelectedItem();
    }

    public void setGitRepositoryRootPath(String select) {
        for (int i = 0; i < gitRepositoryRootPath.getItemCount(); i++) {
            if (select.equals(gitRepositoryRootPath.getItemAt(i))) {
                this.gitRepositoryRootPath.setSelectedItem(select);
                break;
            }
        }
    }

    public String getRemoteGitRepositoryURL() {
        return (String) remoteGitRepositoryURL.getSelectedItem();
    }

    public void setRemoteGitRepositoryURL(String select) {
        for (int i = 0; i < remoteGitRepositoryURL.getItemCount(); i++) {
            if (select.equals(gitRepositoryRootPath.getItemAt(i))) {
                this.gitRepositoryRootPath.setSelectedItem(select);
                break;
            }
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        gitRepositoryRootPath = new JComboBox<>();
        remoteGitRepositoryURL = new JComboBox<>();
    }

    protected void doAction(ActionEvent e) {
        gitRepositoryRootPath.removePropertyChangeListener(this::fillRemoteGitRepositoryURL);
    }
}