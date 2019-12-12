package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.execution.*;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.panels.NonOpaquePanel;
import cucumber.runtime.model.CucumberFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.features.FeatureListManager;

import javax.swing.*;
import java.awt.*;

public class FeatureComboBoxAction extends ComboBoxAction implements DumbAware {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (ActionPlaces.isMainMenuOrActionSearch(e.getPlace())) {
            presentation.setDescription(ExecutionBundle.message("choose.run.configuration.action.description"));
        }

        try {
            if (project == null || project.isDisposed() || !project.isOpen()) {
                updatePresentation(null, null, presentation);
                presentation.setEnabled(false);
            } else {
                updatePresentation(FeatureListManager.getInstance(project).getSelectedFeature(), project, presentation);
                presentation.setEnabled(true);
            }
        } catch (IndexNotReadyException e1) {
            presentation.setEnabled(false);
        }
    }

    private static void updatePresentation(@Nullable CucumberFeature feature,
                                           @Nullable Project project,
                                           @NotNull Presentation presentation) {
        if (project != null && feature != null) {
            String name = feature.getGherkinFeature().getName();
            presentation.setText(name, false);
            setConfigurationIcon(presentation, feature, project);
        }
        else {
            presentation.setText("");
            presentation.setIcon(null);
        }
    }

    private static void setConfigurationIcon(final Presentation presentation,
                                             final CucumberFeature feature,
                                             final Project project) {
        try {
            presentation.setIcon(FeatureListManager.getInstance(project).getFeatureIcon(feature));
        } catch (IndexNotReadyException ignored) {
        }
    }

    @Override
    protected boolean shouldShowDisabledActions() {
        return true;
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(Presentation presentation) {
        ComboBoxButton button = this.createComboBoxButton(presentation);
        button.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        NonOpaquePanel panel = new NonOpaquePanel(new BorderLayout());
        panel.setBorder(IdeBorderFactory.createEmptyBorder(0, 0, 0, 2));
        panel.add(button);
        return panel;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(final JComponent button) {
        DefaultActionGroup allActionsGroup = new DefaultActionGroup();
        Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(button));
        if (project != null) {
            //allActionsGroup.add(ActionManager.getInstance().getAction("editRunConfigurations"));
            /*allActionsGroup.add(new SaveTemporaryAction());
            allActionsGroup.addSeparator();*/
            /*CucumberFeature selected = FeatureListManager.getInstance(project).getSelectedFeature();
            if (selected != null) {
                ExecutionTarget activeTarget = ExecutionTargetManager.getActiveTarget(project);
                Iterator var6 = ExecutionTargetManager.getTargetsToChooseFor(project, selected).iterator();

                while(var6.hasNext()) {
                    ExecutionTarget eachTarget = (ExecutionTarget)var6.next();
                    allActionsGroup.add(new SelectTargetAction(project, eachTarget, eachTarget.equals(activeTarget)));
                }

                allActionsGroup.addSeparator();
            }*/

            for (CucumberFeature feature : FeatureListManager.getInstance(project).getFeatureList()) {
                /*
                ConfigurationType type = var18[var8];
                DefaultActionGroup actionGroup = new DefaultActionGroup();

                    DefaultActionGroup group = entry.getKey() != null ? new DefaultActionGroup((String)entry.getKey(), true) : actionGroup;
                    group.getTemplatePresentation().setIcon(AllIcons.Nodes.Folder);
                    Iterator var15 = ((List)entry.getValue()).iterator();

                    while(var15.hasNext()) {
                        RunnerAndConfigurationSettings settings = (RunnerAndConfigurationSettings)var15.next();
                        group.add(new FeatureComboBoxAction.SelectFeatureAction(settings, project));
                    }

                    if (group != actionGroup) {
                        actionGroup.add(group);
                    }
                }

                allActionsGroup.add(actionGroup);
                allActionsGroup.addSeparator();
                */
                allActionsGroup.add(new SelectFeatureAction(feature, project));
            }
        }
        return allActionsGroup;
    }

    private static class SelectFeatureAction extends DumbAwareAction {
        private final CucumberFeature myFeature;
        private final Project myProject;

        public SelectFeatureAction(final CucumberFeature feature, final Project project) {
            myFeature = feature;
            myProject = project;
            String name = feature.getGherkinFeature().getName();
            if (name.isEmpty()) {
                name = " ";
            }
            final Presentation presentation = getTemplatePresentation();
            presentation.setText(name, false);
            presentation.setDescription("Select '" + name + "'");
            updateIcon(presentation);
        }

        private void updateIcon(final Presentation presentation) {
            setConfigurationIcon(presentation, myFeature, myProject);
        }

        @Override
        public void actionPerformed(@NotNull final AnActionEvent e) {
            FeatureListManager.getInstance(myProject).setSelectedFeature(myFeature);
            updatePresentation(myFeature, myProject, e.getPresentation());
        }

        @Override
        public void update(@NotNull final AnActionEvent e) {
            super.update(e);
            final Presentation presentation = e.getPresentation();
            updateIcon(presentation);
            updateDisabled(presentation);
        }

        private void updateDisabled(Presentation presentation) {
            presentation.setEnabled(FeatureListManager.getInstance(myProject).isEnabled(myFeature));
        }
    }
/*
    private static class SelectTargetAction extends AnAction {
        private final Project myProject;
        private final ExecutionTarget myTarget;

        public SelectTargetAction(Project project, ExecutionTarget target, boolean selected) {
            this.myProject = project;
            this.myTarget = target;
            String name = target.getDisplayName();
            Presentation presentation = this.getTemplatePresentation();
            presentation.setText(name, false);
            presentation.setDescription("Select " + name);
            presentation.setIcon(selected ? RunConfigurationsComboBoxAction.CHECKED_ICON : RunConfigurationsComboBoxAction.EMPTY_ICON);
            presentation.setSelectedIcon(selected ? RunConfigurationsComboBoxAction.CHECKED_SELECTED_ICON : RunConfigurationsComboBoxAction.EMPTY_ICON);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            ExecutionTargetManager.setActiveTarget(this.myProject, this.myTarget);
            updatePresentation(RunManager.getInstance(this.myProject).getSelectedConfiguration(), this.myProject, e.getPresentation());
        }

        public boolean isDumbAware() {
            return Registry.is("dumb.aware.run.configurations");
        }
    }

    private static class SaveTemporaryAction extends DumbAwareAction {
        public SaveTemporaryAction() {
            Presentation presentation = getTemplatePresentation();
            presentation.setIcon(AllIcons.RunConfigurations.SaveTempConfig);
        }

        @Override
        public void actionPerformed(@NotNull final AnActionEvent e) {
            final Project project = e.getData(CommonDataKeys.PROJECT);
            if (project != null) {
                RunnerAndConfigurationSettings settings = chooseTempSettings(project);
                if (settings != null) {
                    final RunManager runManager = RunManager.getInstance(project);
                    runManager.makeStable(settings);
                }
            }
        }

        @Override
        public void update(@NotNull final AnActionEvent e) {
            final Presentation presentation = e.getPresentation();
            final Project project = e.getData(CommonDataKeys.PROJECT);
            if (project == null) {
                disable(presentation);
                return;
            }
            RunnerAndConfigurationSettings settings = chooseTempSettings(project);
            if (settings == null) {
                disable(presentation);
            } else {
                presentation.setText(ExecutionBundle.message("save.temporary.run.configuration.action.name", settings.getName()));
                presentation.setDescription(presentation.getText());
                presentation.setEnabledAndVisible(true);
            }
        }

        private static void disable(final Presentation presentation) {
            presentation.setEnabledAndVisible(false);
        }

        @Nullable
        private static RunnerAndConfigurationSettings chooseTempSettings(@NotNull Project project) {
            RunnerAndConfigurationSettings selectedConfiguration = RunManager.getInstance(project).getSelectedConfiguration();
            if (selectedConfiguration != null && selectedConfiguration.isTemporary()) {
                return selectedConfiguration;
            }
            return ContainerUtil.getFirstItem(RunManager.getInstance(project).getTempConfigurationsList());
        }
    }*/
}