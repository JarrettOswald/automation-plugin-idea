package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SizedIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

public class PluginSettingsAction extends AnAction {
    public PluginSettingsAction() {
        super(JBUI.scale(new SizedIcon(AllIcons.General.Settings, 16, 16)));
    }

    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
            plugin.openOptionsWindow();
        }
    }
}