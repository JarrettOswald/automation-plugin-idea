package ru.lanit.ideaplugin.simplegit;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.util.concurrent.ConcurrentHashMap;

public class SimpleGitPlugin {
    private static ConcurrentHashMap<Project, SimpleGitPlugin> plugins = new ConcurrentHashMap<>();

    private Project project;
    private PropertiesComponent properties;

    private SimpleGitPlugin(Project project) {
        this.project = project;
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        System.out.println("Created new plugin for opened project " + project.getBasePath());
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

    public void gitPush() {
        System.out.println("Git push project " + project.getBasePath());
        String txt= Messages.showInputDialog(project, "What is your name?",
                "Input your name", Messages.getQuestionIcon());
        Messages.showMessageDialog(project, "Hello, " + txt + "!\nI am glad to see you.",
                "Information", Messages.getInformationIcon());
    }
}
