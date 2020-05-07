package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.changes.actions.ScheduleForAdditionAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SizedIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;
import ru.lanit.ideaplugin.simplegit.features.FeatureList;

import java.util.Collections;
import java.util.List;

import static ru.lanit.ideaplugin.simplegit.localization.Language.simpleGitPluginBundle;

public class CreateNewScenarioAction extends AnAction {
    private static final Logger log = Logger.getInstance(CreateNewScenarioAction.class);

    public CreateNewScenarioAction() {
        super(simpleGitPluginBundle.getString("create-new-scenario.action.text"),
                simpleGitPluginBundle.getString("create-new-scenario.action.description"),
                JBUI.scale(new SizedIcon(AllIcons.General.Add, 16, 16)));
    }

    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
            VirtualFile scenarioFile = plugin.createNewScenario();
            if (scenarioFile != null) {
                registerFeatureReadyListener(project, scenarioFile);
                List<VirtualFile> unversionedFiles = Collections.singletonList(scenarioFile);
                ScheduleForAdditionAction.addUnversioned(project, unversionedFiles, this::isStatusForAddition, null);
            }
        }
    }

    public void update(AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            SimpleGitProjectComponent plugin = project.getComponent(SimpleGitProjectComponent.class);
            event.getPresentation().setEnabled(plugin.isPluginActive());
        }
    }

    private void registerFeatureReadyListener(Project project, VirtualFile file) {
        ChangeListManager manager = ChangeListManager.getInstance(project);
        FeatureList featureList = FeatureList.getInstance(project);
        ChangeListAdapter listener = new ChangeListAdapter() {
            @Override public void changeListUpdateDone() {
                Change change = manager.getChange(file);
                if (change != null && change.getFileStatus() == FileStatus.ADDED) {
                    featureList.updateFeaturesAndSelectByFile(file);
                    manager.removeChangeListListener(this);
                }
            }
        };
        manager.addChangeListListener(listener);
    }

    protected boolean isStatusForAddition(FileStatus status) {
        return status == FileStatus.UNKNOWN || status == FileStatus.NOT_CHANGED;
    }
}

