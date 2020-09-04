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
import ru.lanit.ideaplugin.simplegit.features.FeatureList;
import ru.lanit.ideaplugin.simplegit.features.FeatureModel;
import ru.lanit.ideaplugin.simplegit.git.GitManager;
import ru.lanit.ideaplugin.simplegit.git.SynchronizeStatus;

import static ru.lanit.ideaplugin.simplegit.localization.Language.simpleGitPluginBundle;

public class GitSynchronizeAction extends AnAction {
    private static final Logger log = Logger.getInstance(GitSynchronizeAction.class);
    private static SynchronizeStatus status;

    public GitSynchronizeAction() {
        super(simpleGitPluginBundle.getString("git-synchronize.action.text"),
                simpleGitPluginBundle.getString("git-synchronize.action.description"),
                JBUIScale.scaleIcon(new SizedIcon(AllIcons.Actions.Refresh, 16, 16)));
        status = SynchronizeStatus.READY;
    }
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
            plugin.gitSynchronize(event);
        }
    }

    public static void setStatus(SynchronizeStatus st) {
        if (st == SynchronizeStatus.READY || st.ordinal() > status.ordinal()) {
            status = st;
        }
    }

    public void update(AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
            if (plugin.isPluginActive() && status == SynchronizeStatus.READY) {
                event.getPresentation().setEnabled(true);
            } else {
                event.getPresentation().setEnabled(false);
                if (status != SynchronizeStatus.READY) {
                    GitManager manager = plugin.getGitManager();
                    if (status == SynchronizeStatus.UPDATED) {
                        FeatureModel feature = FeatureList.getInstance(project).getSelectedFeature();
                        boolean canCommit = false;
                        if (feature != null) {
                            canCommit = feature.getJiraTag() != null;
                        }
                        if (canCommit) {
                            manager.commit();
                        } else {
                            setStatus(SynchronizeStatus.READY);
                        }
                    }
                    if (status == SynchronizeStatus.COMMITED)
                        manager.pushGit();
                }
            }
        }
    }
}
