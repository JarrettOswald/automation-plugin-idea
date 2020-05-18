package ru.lanit.ideaplugin.simplegit;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.dialogs.newfeature.NewFeatureDialog;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;
import ru.lanit.ideaplugin.simplegit.features.FeatureList;
import ru.lanit.ideaplugin.simplegit.features.ScenarioType;
import ru.lanit.ideaplugin.simplegit.git.GitManager;
import ru.lanit.ideaplugin.simplegit.settings.ProjectSettings;
import ru.lanit.ideaplugin.simplegit.settings.SettingsProvider;
import ru.lanit.ideaplugin.simplegit.tags.model.EditableFavoriteTagList;
import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.FavoriteTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.TagType;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleGitProjectComponent implements ProjectComponent {
    private static final Logger log = Logger.getInstance(SimpleGitProjectComponent.class);

    public static ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
    public static PluginId pluginId = PluginManagerCore.getPluginByClassName(SimpleGitProjectComponent.class.getCanonicalName());
    public static IdeaPluginDescriptor ideaPluginDescriptor;
    static {
        ideaPluginDescriptor = Arrays.stream(PluginManagerCore.getPlugins()).filter(plugin -> plugin.getPluginId().equals(pluginId)).findFirst().get();
    }

    private final Project project;
    private FeatureList featureList;
    private SettingsProvider settings;
    private GitManager gitManager;
    private RefreshSession refreshSession;
    private PropertyChangeListener settingsChangeListener;

    public SimpleGitProjectComponent(Project project) {
        this.project = project;
        gitManager = new GitManager(this);
//        this.refreshSession = RefreshQueue.getInstance().createSession(true, true, null);
        settings = new SettingsProvider(project);
        settings.restoreAllSettings();
        settingsChangeListener = new SettingsChangeListener();
        settings.addPropertyChangeListener(settingsChangeListener);
    }

    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "SimpleGit";
    }

    public void projectOpened() {
        // called when project is opened
        this.featureList = ServiceManager.getService(project, FeatureList.class);
    }

    public Project getProject() {
        return project;
    }

    public VirtualFile createNewScenario() {
        System.out.println("Create new scenario in project " + project.getBasePath());
        NewFeatureDialog newFeatureDialog = new NewFeatureDialog(project);
        settings.setSettingsToNewFeatureDialog(newFeatureDialog);
        newFeatureDialog.show();
        if (newFeatureDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            File file = new File(getFeaturePath(), newFeatureDialog.getFeatureFilename());
            try {
                if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
                    if (file.createNewFile()) {
                        FileOutputStream outputStream = new FileOutputStream(file);
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                        PrintWriter printWriter = new PrintWriter(outputStreamWriter, true);
                        printWriter.println("# language: ru\n");
                        printWriter.printf("Функционал: %s\n\n", newFeatureDialog.getFeatureName());
                        Map<TagType, List<String>> tags = newFeatureDialog.getFeatureTags().stream().sorted()
                                .collect(Collectors.groupingBy(TagType::getTagTypeByTag, Collectors.mapping(AbstractTag::getTagAsString, Collectors.toList())));
                        Arrays.stream(TagType.values())
                                .map(tags::get).filter(Objects::nonNull)
                                .map(tagList -> "    " + String.join(" ", tagList))
                                .forEachOrdered(printWriter::println);
                        printWriter.printf("    %s: %s\n",
                                newFeatureDialog.getScenarioType().getName(),
                                newFeatureDialog.getScenarioName());
                        printWriter.println("      #Сценарий");
                        if (newFeatureDialog.getScenarioType() == ScenarioType.SCENARIO_OUTLINE) {
                            printWriter.println("\n    Примеры:\n      | Имя1 | Имя2 |\n      |      |      |");
                        }
                        printWriter.close();
                        VirtualFileSystem fileSystem = LocalFileSystem.getInstance();
                        VirtualFile virtualFile = fileSystem.refreshAndFindFileByPath(file.getAbsolutePath());
                        if (virtualFile != null) FileEditorManager.getInstance(project).openFile(virtualFile, true);
                        return virtualFile;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void gitSynchronize(AnActionEvent event) {
        System.out.println("Git synchronize project " + project.getBasePath());
//        featureList.updateFeatures();
        gitManager.synchronizeGit(event);
    }
    public void openOptionsWindow() {
        System.out.println("Open settings dialog for project " + project.getBasePath());
        PluginSettingsDialog pluginSettingsDialog = new PluginSettingsDialog(project);
        if (settings.getRemoteGitRepositoryURL().equals("")) {
            gitManager.suggestRepository(settings);
        }
        settings.setSettingsToDialog(pluginSettingsDialog);
        pluginSettingsDialog.show();
        if (pluginSettingsDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            settings.setSettingsFromDialog(pluginSettingsDialog);
//            Messages.showMessageDialog(project, "Selected feature path: " + pluginSettingsDialog.getFeaturePath(),
//                    "Information", Messages.getInformationIcon());
        }
    }

    public boolean isPluginActive() {
        return settings.isPluginActive();
    }

    public EditableFavoriteTagList getFavoriteTags() {
        return settings.getFavoriteTags();
    }

    public String getFeaturePath() {
        System.out.println("getFeaturePath");
        return getFeatureDir().getPath();
    }

    public VirtualFile getFeatureDir() {
        System.out.println("getFeatureDir");
        String basePath = project.getBasePath();
        VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
        return baseDir.findFileByRelativePath(settings.getFeaturePath());
    }

    public void updateFeatures() {
        featureList.updateFeatures();
        /*
        final SvnVcs vcs = SvnVcs.getInstance(project);
        VirtualFile file = getFeatureDir().findChild("a.feature");
        if (file != null) {
            final File ioFile = virtualToIoFile(file);
            try {
                new RepeatSvnActionThroughBusy() {
                    @Override
                    protected void executeImpl() throws VcsException {
                        vcs.getFactory(ioFile).createAddClient().add(ioFile, null, false, false, true, null);
                    }
                }.execute();
                VcsDirtyScopeManager.getInstance(project).fileDirty(file);
            } catch (VcsException e) {
                exceptions.add(e);
            }
        }*/
    }

    public void projectClosed() {
        // called when project is being closed
    }

    public GitManager getGitManager() {
        return this.gitManager;
    }

    public String getRemoteGitRepositoryURL() {
        return settings.getRemoteGitRepositoryURL();
    }

    public String getGitRepositoryRootPath() {
        return settings.getGitRepositoryRootPath();
    }

    public String getRemoteMainBranch() {
        return settings.getRemoteMainBranch();
    }

    private class SettingsChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (ProjectSettings.FEATURE_PATH.equals(propertyName) && isPluginActive()) {
                featureList.updateFeatures();
            }
        }
    }
}