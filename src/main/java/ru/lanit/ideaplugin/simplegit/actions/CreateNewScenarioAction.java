package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SizedIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

import java.util.Collections;
import java.util.List;

import static ru.lanit.ideaplugin.simplegit.localization.Language.simpleGitPluginBundle;

public class CreateNewScenarioAction extends AnAction {
    private static final Logger log = Logger.getInstance(CreateNewScenarioAction.class);
    private static boolean listenerRegistered = false;

    public CreateNewScenarioAction() {
        super(simpleGitPluginBundle.getString("create-new-scenario.action.text"),
                simpleGitPluginBundle.getString("create-new-scenario.action.description"),
                JBUI.scale(new SizedIcon(AllIcons.General.Add, 16, 16)));
    }

    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
            plugin.createNewScenario();
        }
    }

    public void update(AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
            event.getPresentation().setEnabled(plugin.isPluginActive());
        }
    }
}

