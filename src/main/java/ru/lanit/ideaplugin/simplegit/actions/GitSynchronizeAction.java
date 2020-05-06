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

import static ru.lanit.ideaplugin.simplegit.localization.Language.simpleGitPluginBundle;

public class GitSynchronizeAction extends AnAction {
    private static final Logger log = Logger.getInstance(GitSynchronizeAction.class);

    public GitSynchronizeAction() {
        super(simpleGitPluginBundle.getString("git-synchronize.action.text"),
                simpleGitPluginBundle.getString("git-synchronize.action.description"),
                JBUI.scale(new SizedIcon(AllIcons.Actions.Refresh, 16, 16)));
    }
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
            plugin.gitSynchronize(event);
        }
    }

    public void update(AnActionEvent event) {
//        JFrame frame = new JFrame();
//        JBPopupFactory.getInstance().createComponentPopupBuilder(frame, frame);
//        Project project = event.getData(CommonDataKeys.PROJECT);
//        if (project != null) {
//            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
//            event.getPresentation().setEnabled(plugin.isPluginActive());
//        }
    }
}
