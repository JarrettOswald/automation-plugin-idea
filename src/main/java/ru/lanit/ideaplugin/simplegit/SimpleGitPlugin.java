package ru.lanit.ideaplugin.simplegit;

import com.google.common.collect.Lists;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberTagStatement;
import gherkin.formatter.model.Feature;
import ru.lanit.ideaplugin.simplegit.dialogs.NewScenarioDialog;
import ru.lanit.ideaplugin.simplegit.dialogs.PluginOptionsDialog;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.icons.AllIcons.Json.Array;

public class SimpleGitPlugin {
    private static ConcurrentHashMap<Project, SimpleGitPlugin> plugins = new ConcurrentHashMap<>();

    private Project project;
    private PropertiesComponent properties;
    private List<CucumberFeature> features;
    private ScenarioList scenarioList;

    private RefreshSession refreshSession;

    private SimpleGitPlugin(Project project) {
        this.project = project;
//        this.refreshSession = RefreshQueue.getInstance().createSession(true, true, null);
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        System.out.println("Created new plugin for opened project " + project.getBasePath());
    }

    public static SimpleGitPlugin getPluginFor(Project project) {
        System.out.println("Try get plugin for opened project " + project.getBasePath());
        return plugins.computeIfAbsent(project, SimpleGitPlugin::new);
    }

    public static SimpleGitPlugin getPluginFor(AnActionEvent event) {
        return getPluginFor(event.getProject());
    }

    public static void registerScenarioComboBox(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getProject();
        if (project == null) {
            presentation.setEnabled(false);
        } else {
            SimpleGitPlugin plugin = getPluginFor(project);
            if (plugin.scenarioList == null) {
                plugin.scenarioList = ScenarioList.getScenarioListFor(presentation);
                plugin.scenarioList.registerPlugin(plugin);
            }
        }
    }

    public Project getProject() {
        return project;
    }

    public String getTaskName() {
        return properties.getValue("taskName");
    }

    public void setTaskName(String taskName) {
        properties.setValue("taskName", taskName);
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
        scenarioList.updateFeatures();
    }

    public void openOptionsWindow() {
        System.out.println("Create new scenario in project " + project.getBasePath());
        PluginOptionsDialog newScenarioDialog = new PluginOptionsDialog(project);
        newScenarioDialog.show();
        if (newScenarioDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            Messages.showMessageDialog(project, "Selected feature catalog: " + newScenarioDialog.getFeatureCatalog(),
                    "Information", Messages.getInformationIcon());
        }
    }
}
