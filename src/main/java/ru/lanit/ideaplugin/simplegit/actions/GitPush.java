package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.IconLoader;
import com.sun.istack.internal.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

public class GitPush extends AnAction {

    public GitPush() {
         super(IconLoader.getIcon("/upload-icon.png"));
    }

    public void actionPerformed(@NotNull AnActionEvent event) {
        SimpleGitPlugin.getPluginFor(event).gitPush();
    }
}
