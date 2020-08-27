package ru.lanit.ideaplugin.simplegit.features;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBus;
import gherkin.formatter.model.TagStatement;
import gherkin.parser.Parser;
import gherkin.util.FixJava;
import git4idea.GitLocalBranch;
import git4idea.actions.GitAdd;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;
import ru.lanit.ideaplugin.simplegit.git.GitManager;
import ru.lanit.ideaplugin.simplegit.tags.tag.JiraTag;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FeatureListImpl extends FeatureList implements BulkFileListener {
    private static final Logger log = Logger.getInstance(FeatureListImpl.class);

    private final Project project;
    private final SimpleGitProjectComponent plugin;
    private List<FeatureModel> featureList;
    private FeatureModel selectedFeature;
    private boolean editFeature;

    public FeatureListImpl(Project project) {
        this.project = project;
        this.plugin = project.getComponent(SimpleGitProjectComponent.class);
        //TODO: check feature is dirty on startup
        this.editFeature = false;
        updateFeatures(null);
        plugin.getProject().getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    private FeatureModel readFeatureFile(String path) {
        FeatureFormatter formatter = new FeatureFormatter(plugin);
        try {
            String gherkin = FixJava.readReader(new InputStreamReader(
                    new FileInputStream(path), "UTF-8"));
            System.out.println("gherkin...\n" + gherkin);
            Parser parser = new Parser(formatter, false);
            parser.parse(gherkin, path, 0);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        FeatureModel model = formatter.getFeatureModel();
        if (model != null && model.getFeature() != null && model.getFeature().getName() != null) {
            return model;
        }
        return null;
    }

    private void updateFeaturesList() {
        List<FeatureModel> features = new ArrayList<>();
//        try {
//            features = CucumberFeature.load(
//                    new FileResourceLoader(), Collections.singletonList(plugin.getFeaturePath()), Collections.emptyList());
//        } catch (CucumberException e) {
//            features = new ArrayList<>();
//        }
        VfsUtil.processFilesRecursively(plugin.getFeatureDir(), file -> {
            if ("feature".equals(file.getExtension())) {
                FeatureModel feature = readFeatureFile(file.getPath());
                if (feature != null) {
                    features.add(feature);
                }
            }
            return true;
        });
        features.sort((f1, f2) -> {
            int feature = f1.getFeature().getName().compareTo(f2.getFeature().getName());
            if (feature == 0) {
                List<TagStatement> s1 = f1.getScenarioList();
                List<TagStatement> s2 = f2.getScenarioList();
                if (s1.size() > 0) {
                    return s2.size() > 0 ? s1.get(0).getName().compareTo(s2.get(0).getName()) : -1;
                } else {
                    return s2.size() > 0 ? 1 : 0;
                }
            } else {
                return feature;
            }
        }); /*
        for (FeatureModel feature : features) {
            System.out.println("New feature found at " + feature.getPath());
//            System.out.println("  Language: " + feature.getI18n().getIsoCode());
            System.out.println("  Name    : " + feature.getFeature().getName());
            for (TagStatement segment : feature.getScenarioList()) {
                System.out.println("    " + segment.getKeyword() + ": " + segment.getName());
            }
        }*/
        this.featureList = features;
    }

    @Override
    public void updateFeatures(AnActionEvent event) {
        updateFeaturesList();
        FeatureModel select = null;
        if (selectedFeature != null) {
            String path = selectedFeature.getPath();
            select = featureList.stream()
                    .filter(feature -> feature.getPath().equals(path))
                    .findFirst().orElse(null);
        }
        if (select != null) selectFeature(select, event);
    }

    @Override
    public void updateFeaturesAndSelectByFile(VirtualFile file, AnActionEvent event) {
        updateFeaturesList();
        String filename = file.getPath();
        FeatureModel select = featureList.stream()
                .filter(feature -> feature.getPath().equals(filename))
                .findFirst().orElse(null);

        if (select == null && selectedFeature != null) {
            String path = selectedFeature.getPath();
            select = featureList.stream()
                    .filter(feature -> feature.getPath().equals(path))
                    .findFirst().orElse(null);
        }
        if (select != null) selectFeature(select, event);
    }

    public void addNewFiles() {
        final RefreshSession session = RefreshQueue.getInstance().createSession(true, true, () -> {
            // here in callback files are already known to VFS and now we can add them
            final ChangeListManagerImpl changeListManager = ChangeListManagerImpl.getInstanceImpl(project);
//            changeListManager.addUnversionedFiles(changeListManager.getDefaultChangeList(), unversionedFiles);
            // files will be visible in change list manager after its inner update. not synchronously after this call
        });

        session.addAllFiles(plugin.getFeatureDir());
        session.launch();     // starts refresh
    }

    @Override
    public FeatureModel getSelectedFeature() {
        return selectedFeature;
    }

    private void selectFeature(FeatureModel selectedFeature, AnActionEvent event) {
        this.selectedFeature = selectedFeature;
        if (selectedFeature != null) {
            VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(selectedFeature.getPath());
            boolean unv = ChangeListManagerImpl.getInstanceImpl(project).getUnversionedFiles().stream().anyMatch(unversioned -> unversioned.equals(file));
            if (unv) {
                if (event != null) new GitAdd().actionPerformed(event);
//                event.getData(VcsDataKeys.VIRTUAL_FILE_STREAM).
//                List<VirtualFile> unversionedFiles = Collections.singletonList(file);
//                ScheduleForAdditionAction.addUnversioned(project, unversionedFiles, status -> true, null);
            }
            JiraTag jiraTag = selectedFeature.getJiraTag();
            String branchName = null;
            if (jiraTag != null) {
                branchName = "feature/" + jiraTag.getName().toLowerCase();
            } else {
                GitLocalBranch branch = plugin.getGitManager().getLocalBranchByRemoteName(plugin.getRemoteMainBranch());
                if (branch != null) branchName = branch.getName();
            }
            if (branchName != null) {
                GitManager manager = plugin.getGitManager();
                GitLocalBranch currentBranch = manager.getCurrentBranch();
                if (!currentBranch.getName().equalsIgnoreCase(branchName)) {
                    manager.checkoutExistingOrNewBranch(branchName, null);
                }
            }
        }
    }
    @Override
    public void setSelectedFeature(FeatureModel selectedFeature, AnActionEvent event) {
        selectFeature(selectedFeature, event);
        VirtualFileSystem fileSystem = LocalFileSystem.getInstance();
        VirtualFile file = fileSystem.refreshAndFindFileByPath(selectedFeature.getPath());
        if (file != null)
            FileEditorManager.getInstance(project).openFile(file, true);
    }

    @Override
    public FeatureState getFeatureState(@NotNull FeatureModel feature) {
        if (feature == selectedFeature) {
            return editFeature ? FeatureState.EDITED : FeatureState.SELECTED;
        }
        return editFeature ? FeatureState.BLOCKED : FeatureState.SELECTABLE;
    }

    @Override
    public List<FeatureModel> getFeatureList() {
        return featureList;
    }

    @Override
    public boolean isEnabledFeature(FeatureModel myFeature) {
        return myFeature == selectedFeature || !editFeature;
    }

    /////////////////////////////////////////////////////////////////////////

    public void before(@NotNull List<? extends VFileEvent> events) {
        System.out.println("Updating features");
        this.updateFeatures(null);
    }

    public void after(@NotNull List<? extends VFileEvent> events) {
        System.out.println("Updating features");
        this.updateFeatures(null);
    }
}
