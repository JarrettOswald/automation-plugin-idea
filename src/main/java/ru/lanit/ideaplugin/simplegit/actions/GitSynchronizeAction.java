package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

public class GitSynchronizeAction extends AnAction {

    public void actionPerformed(@NotNull AnActionEvent event) {
        SimpleGitPlugin.getPluginFor(event).ifPresent(SimpleGitPlugin::gitSynchronize);
    }

    public void update(AnActionEvent event) {
        SimpleGitPlugin.getPluginFor(event).ifPresent(
                plugin -> event.getPresentation().setEnabled(plugin.isPluginActive())
        );
    }
}
