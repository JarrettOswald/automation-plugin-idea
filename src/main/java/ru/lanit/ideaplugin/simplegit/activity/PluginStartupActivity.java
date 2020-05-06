package ru.lanit.ideaplugin.simplegit.activity;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NonNls;

public class PluginStartupActivity implements StartupActivity {
    private static final Logger log = Logger.getInstance(PluginStartupActivity.class);

    @Override
    @NonNls
    public void runActivity(Project project) {
//        project.getMessageBus().connect().subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, new VcsListener() {});
        System.out.println("Opened project " + project.getBasePath());
//        SimpleGitPlugin.getPluginFor(project);

//            DumbService.getInstance(project).runWhenSmart()
    }
}
