package ru.lanit.ideaplugin.simplegit;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import ru.lanit.ideaplugin.simplegit.dialogs.newfeature.NewFeatureDialog;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;
import ru.lanit.ideaplugin.simplegit.settings.SettingsChangeListener;
import ru.lanit.ideaplugin.simplegit.settings.PluginSettings;
import ru.lanit.ideaplugin.simplegit.settings.PluginSettingsProvider;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleGitPlugin implements SettingsChangeListener {
    private static ConcurrentHashMap<Project, SimpleGitPlugin> plugins = new ConcurrentHashMap<>();

    private Project project;
    private PluginSettingsProvider settings;
    private FeatureList featureList;

    private RefreshSession refreshSession;

    private SimpleGitPlugin(Project project) {
        this.project = project;
//        this.refreshSession = RefreshQueue.getInstance().createSession(true, true, null);
        settings = new PluginSettingsProvider(project, this);
        settings.restoreAllSettings();
//        System.out.println("Created new plugin for opened project " + project.getBasePath());
    }

    public static SimpleGitPlugin getPluginFor(Project project) {
//        System.out.println("Try get plugin for opened project " + project.getBasePath());
        return plugins.computeIfAbsent(project, SimpleGitPlugin::new);
    }

    public static Optional<SimpleGitPlugin> getPluginFor(AnActionEvent event) {
        return Optional.ofNullable(event.getProject()).map(SimpleGitPlugin::getPluginFor);
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
        Messages.showMessageDialog(project, "Push is not implemented",
                "Information", Messages.getInformationIcon());
        featureList.updateFeatures();
    }

    public void openOptionsWindow() {
        System.out.println("Open settings dialog for project " + project.getBasePath());
        PluginSettingsDialog pluginSettingsDialog = new PluginSettingsDialog(project);
        settings.setSettingsToDialog(pluginSettingsDialog);
        pluginSettingsDialog.show();
        if (pluginSettingsDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            settings.setSettingsFromDialog(pluginSettingsDialog);
            Messages.showMessageDialog(project, "Selected feature path: " + pluginSettingsDialog.getFeaturePath(),
                    "Information", Messages.getInformationIcon());
        }
    }

    @Override
    public void onSettingsChange(PluginSettings newOptions, PluginSettings oldOptions) {
        if (oldOptions != null && newOptions.isPluginActive() && !newOptions.getFeaturePath().equals(oldOptions.getFeaturePath())) {
            featureList.updateFeatures();
        }
    }

    public boolean isPluginActive() {
        return settings.isPluginActive();
    }

    public void registerFeatureComboBox(Presentation presentation) {
        if (featureList == null) {
            featureList = FeatureList.getFeatureListFor(presentation);
            featureList.registerPlugin(this);
        }
        presentation.setEnabled(isPluginActive());
    }

    public String getFeaturePath() {
        return project.getBaseDir().findFileByRelativePath(settings.getFeaturePath()).getPath();
    }

    public VirtualFile getFeatureDir() {
        return LocalFileSystem.getInstance().findFileByPath(getFeaturePath());
    }
}
