package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SizedIcon;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

import static ru.lanit.ideaplugin.simplegit.localization.Language.simpleGitPluginBundle;

public class PluginSettingsAction extends AnAction {
    private static final Logger log = Logger.getInstance(PluginSettingsAction.class);

    public PluginSettingsAction() {
        super(simpleGitPluginBundle.getString("plugin-settings.action.text"),
                simpleGitPluginBundle.getString("plugin-settings.action.description"),
                JBUIScale.scaleIcon(new SizedIcon(AllIcons.General.Settings, 16, 16)));
    }

    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
            plugin.openOptionsWindow();
        }
    }
}
