package ru.lanit.ideaplugin.simplegit;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import cucumber.runtime.model.CucumberFeature;
import ru.lanit.ideaplugin.simplegit.dialogs.newfeature.NewScenarioDialog;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;
import ru.lanit.ideaplugin.simplegit.settings.SettingsChangeListener;
import ru.lanit.ideaplugin.simplegit.settings.PluginSettings;
import ru.lanit.ideaplugin.simplegit.settings.PluginSettingsManager;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleGitPlugin implements SettingsChangeListener {
    private static ConcurrentHashMap<Project, SimpleGitPlugin> plugins = new ConcurrentHashMap<>();

    private Project project;
    private PropertiesComponent properties;
    private PluginSettingsManager options;
    private List<CucumberFeature> features;
    private FeatureList featureList;

    private RefreshSession refreshSession;

    private SimpleGitPlugin(Project project) {
        this.project = project;
//        this.refreshSession = RefreshQueue.getInstance().createSession(true, true, null);
        options = new PluginSettingsManager(project, this);
        options.restoreAllSettings();
        System.out.println("Created new plugin for opened project " + project.getBasePath());
    }

    public static SimpleGitPlugin getPluginFor(Project project) {
        System.out.println("Try get plugin for opened project " + project.getBasePath());
        return plugins.computeIfAbsent(project, SimpleGitPlugin::new);
    }

    public static SimpleGitPlugin getPluginFor(AnActionEvent event) {
        return getPluginFor(event.getProject());
    }

    public static void registerFeatureComboBox(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getProject();
        if (project == null) {
            presentation.setEnabled(false);
        } else {
            SimpleGitPlugin plugin = getPluginFor(project);
            if (plugin.featureList == null) {
                plugin.featureList = FeatureList.getScenarioListFor(presentation);
                plugin.featureList.registerPlugin(plugin);
            }
        }
    }

    public Project getProject() {
        return project;
    }

    public void createNewScenario() {
        System.out.println("Create new scenario in project " + project.getBasePath());
        NewScenarioDialog newScenarioDialog = new NewScenarioDialog(project);
        newScenarioDialog.show();

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
        options.setSettingsToDialog(pluginSettingsDialog);
        pluginSettingsDialog.show();
        if (pluginSettingsDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            options.setSettingsFromDialog(pluginSettingsDialog);
            Messages.showMessageDialog(project, "Selected feature catalog: " + pluginSettingsDialog.getFeatureCatalog(),
                    "Information", Messages.getInformationIcon());
        }
    }

    @Override
    public void onSettingsChange(PluginSettings newOptions, PluginSettings oldOptions) {
        if (newOptions.isPluginActive()) {

        }
    }

    public boolean isPluginActive() {
        return options.isPluginActive();
    }
}
