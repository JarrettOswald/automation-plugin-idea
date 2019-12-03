package ru.lanit.ideaplugin.simplegit;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberTagStatement;
import ru.lanit.ideaplugin.simplegit.dialogs.NewScenarioDialog;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleGitPlugin {
    private static ConcurrentHashMap<Project, SimpleGitPlugin> plugins = new ConcurrentHashMap<>();

    private Project project;
    private PropertiesComponent properties;
    private List<CucumberFeature> features;

    private SimpleGitPlugin(Project project) {
        this.project = project;
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        System.out.println("Created new plugin for opened project " + project.getBasePath());
        updateFeatures();
    }

    public List<CucumberFeature> getFeatures() {
        return features;
    }

    public void updateFeatures() {
        features = CucumberFeature.load(
                new FileResourceLoader(), Collections.singletonList(project.getBasePath()), Collections.emptyList());
        for (CucumberFeature feature : features) {
            System.out.println("New feature found at " + feature.getPath());
            System.out.println("  Language: " + feature.getI18n().getIsoCode());
            System.out.println("  Name    : " + feature.getGherkinFeature().getName());
            for (CucumberTagStatement segment : feature.getFeatureElements()) {
                System.out.println("    " + segment.getGherkinModel().getKeyword() + ": " + segment.getGherkinModel().getName());
            }
        }
    }

    public static SimpleGitPlugin getPluginFor(Project project) {
        System.out.println("Try get plugin for opened project " + project.getBasePath());
        return plugins.computeIfAbsent(project, SimpleGitPlugin::new);
    }

    public static SimpleGitPlugin getPluginFor(AnActionEvent event) {
        return getPluginFor(event.getData(PlatformDataKeys.PROJECT));
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
        updateFeatures();
    }
}
