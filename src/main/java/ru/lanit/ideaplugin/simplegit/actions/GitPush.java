package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;
import ru.lanit.ideaplugin.simplegit.dialogs.NewScenarioDialog;

public class GitPush extends AnAction {

    public GitPush() {
         super(IconLoader.getIcon("/upload-icon.png"));
    }

    public void actionPerformed(@NotNull AnActionEvent event) {
        NewScenarioDialog newScenarioDialog = new NewScenarioDialog(event.getData(PlatformDataKeys.PROJECT));
        newScenarioDialog.show();
        SimpleGitPlugin.getPluginFor(event).gitPush();
    }
}
