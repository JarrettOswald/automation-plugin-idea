package ru.lanit.ideaplugin.simplegit;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.dialogs.newfeature.NewFeatureDialog;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;
import ru.lanit.ideaplugin.simplegit.features.FeatureList;
import ru.lanit.ideaplugin.simplegit.settings.PluginSettings;
import ru.lanit.ideaplugin.simplegit.settings.PluginSettingsProvider;
import ru.lanit.ideaplugin.simplegit.settings.SettingsChangeListener;

public class SimpleGitProjectComponent implements ProjectComponent, SettingsChangeListener {
    private final Project project;
    private FeatureList featureList;
    private PluginSettingsProvider settings;
    private GitManager gitManager;
    private RefreshSession refreshSession;

    public SimpleGitProjectComponent(Project project) {
        this.project = project;
        gitManager = new GitManager(this);
//        this.refreshSession = RefreshQueue.getInstance().createSession(true, true, null);
        settings = new PluginSettingsProvider(project, this);
        settings.restoreAllSettings();
    }

    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "SimpleGit";
    }

    public void projectOpened() {
        // called when project is opened
        this.featureList = ServiceManager.getService(project, FeatureList.class);
    }

    public Project getProject() {
        return project;
    }

    public void createNewScenario() {
        System.out.println("Create new scenario in project " + project.getBasePath());
        NewFeatureDialog newFeatureDialog = new NewFeatureDialog(project);
        newFeatureDialog.show();

    }

    public void gitSynchronize() {
        System.out.println("Git synchronize project " + project.getBasePath());
        /*String txt= Messages.showInputDialog(project, "What is your name?",
                "Input your name", Messages.getQuestionIcon());*/
//        Messages.showMessageDialog(project, "Push is not implemented",
//                "Information", Messages.getInformationIcon());
        featureList.updateFeatures();
    }
    public void openOptionsWindow() {
        System.out.println("Open settings dialog for project " + project.getBasePath());
        PluginSettingsDialog pluginSettingsDialog = new PluginSettingsDialog(project);
        if (settings.getRemoteGitRepositoryURL() == "") {
            gitManager.suggestRepository(settings);
        }
        settings.setSettingsToDialog(pluginSettingsDialog);
        pluginSettingsDialog.show();
        if (pluginSettingsDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            settings.setSettingsFromDialog(pluginSettingsDialog);
            Messages.showMessageDialog(project, "Selected feature path: " + pluginSettingsDialog.getFeaturePath(),
                    "Information", Messages.getInformationIcon());
        }
    }

    public boolean isPluginActive() {
        return settings.isPluginActive();
    }

    public String getFeaturePath() {
        System.out.println("getFeaturePath");
        return getFeatureDir().getPath();
    }

    public VirtualFile getFeatureDir() {
        System.out.println("getFeatureDir");
        String basePath = project.getBasePath();
        VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
        return baseDir.findFileByRelativePath(settings.getFeaturePath());
    }

    @Override
    public void onSettingsChange(PluginSettings newOptions, PluginSettings oldOptions) {
        if (oldOptions != null && newOptions.isPluginActive() && !newOptions.getFeaturePath().equals(oldOptions.getFeaturePath())) {
            featureList.updateFeatures();
        }
    }

    public void projectClosed() {
        // called when project is being closed
    }

    public GitManager getGitManager() {
        return this.gitManager;
    }
}