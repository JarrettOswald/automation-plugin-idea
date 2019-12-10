package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

public class CreateNewScenarioAction extends AnAction {

    public void actionPerformed(@NotNull AnActionEvent event) {
        SimpleGitPlugin.getPluginFor(event).createNewScenario();
    }
}

