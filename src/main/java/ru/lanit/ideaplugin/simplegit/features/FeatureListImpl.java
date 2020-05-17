package ru.lanit.ideaplugin.simplegit.features;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vcs.changes.actions.ScheduleForAdditionAction;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBus;
import gherkin.formatter.model.TagStatement;
import gherkin.parser.Parser;
import gherkin.util.FixJava;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
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
        updateFeatures();
        plugin.getProject().getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    private FeatureModel readFeatureFile(String path) {
        try {
            String gherkin = FixJava.readReader(new InputStreamReader(
                    new FileInputStream(path), "UTF-8"));
            System.out.println("gherkin...\n" + gherkin);
            FeatureFormatter formatter = new FeatureFormatter(plugin);
            Parser parser = new Parser(formatter, false);
            parser.parse(gherkin, path, 0);
            return formatter.getFeatureModel();
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
                TagStatement s1 = f1.getScenarioList().get(0);
                TagStatement s2 = f2.getScenarioList().get(0);
                if (s1 != null) {
                    return s2 != null ? s1.getName().compareTo(s2.getName()) : -1;
                } else {
                    return s2 != null ? 1 : 0;
                }
            } else {
                return feature;
            }
        });
        for (FeatureModel feature : features) {
            System.out.println("New feature found at " + feature.getPath());
//            System.out.println("  Language: " + feature.getI18n().getIsoCode());
            System.out.println("  Name    : " + feature.getFeature().getName());
            for (TagStatement segment : feature.getScenarioList()) {
                System.out.println("    " + segment.getKeyword() + ": " + segment.getName());
            }
        }
        this.featureList = features;
    }

    @Override
    public void updateFeatures() {
        updateFeaturesList();
        FeatureModel select = null;
        if (selectedFeature != null) {
            String path = selectedFeature.getPath();
            select = featureList.stream()
                    .filter(feature -> feature.getPath().equals(path))
                    .findFirst().orElse(null);
        }
        this.selectedFeature = select;
    }

    @Override
    public void updateFeaturesAndSelectByFile(VirtualFile file) {
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
        this.selectedFeature = select;
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

    @Override
    public void setSelectedFeature(FeatureModel selectedFeature) {
        selectFeature(selectedFeature);
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
        this.updateFeatures();
    }

    public void after(@NotNull List<? extends VFileEvent> events) {
        System.out.println("Updating features");
        this.updateFeatures();
    }
}
