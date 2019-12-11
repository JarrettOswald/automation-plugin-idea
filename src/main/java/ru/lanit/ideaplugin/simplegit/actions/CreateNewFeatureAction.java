package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

public class CreateNewFeatureAction extends AnAction {

    public void actionPerformed(@NotNull AnActionEvent event) {
        SimpleGitPlugin.getPluginFor(event).createNewScenario();
    }

    public void update(AnActionEvent event) {
        event.getPresentation().setEnabled(SimpleGitPlugin.getPluginFor(event).isPluginActive());
    }
}

