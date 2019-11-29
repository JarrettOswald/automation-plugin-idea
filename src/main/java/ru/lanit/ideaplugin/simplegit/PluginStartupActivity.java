package ru.lanit.ideaplugin.simplegit;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsListener;

public class PluginStartupActivity implements StartupActivity {
    @Override
    public void runActivity(Project project) {
//        project.getMessageBus().connect().subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, new VcsListener() {});
        System.out.println("Opened project " + project.getBasePath());
        SimpleGitPlugin.getPluginFor(project);
//            DumbService.getInstance(project).runWhenSmart()
    }
}
