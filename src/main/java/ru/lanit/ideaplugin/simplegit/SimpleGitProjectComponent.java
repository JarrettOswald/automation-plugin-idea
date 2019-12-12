package ru.lanit.ideaplugin.simplegit;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.features.FeatureListManager;

public class SimpleGitProjectComponent implements ProjectComponent {
    private final Project project;
    private FeatureListManager featureListManager;

    public SimpleGitProjectComponent(Project project) {
        this.project = project;
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
        this.featureListManager = ServiceManager.getService(project, FeatureListManager.class);
    }

    public void projectClosed() {
        // called when project is being closed
    }
}