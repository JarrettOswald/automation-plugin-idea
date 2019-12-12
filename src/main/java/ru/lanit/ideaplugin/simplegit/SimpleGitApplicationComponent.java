package ru.lanit.ideaplugin.simplegit;

import com.intellij.openapi.project.Project;

public class SimpleGitApplicationComponent {
    public String[] searchFeatures(Project project) {
        if (project != null) {
            return new String[] {project.getName() + " feature 1", project.getName() + " feature 2", project.getName() + " feature 2"};
        }
        return new String[]{};
    }

    public void selectFeature(String selectedItem) {
    }
}
