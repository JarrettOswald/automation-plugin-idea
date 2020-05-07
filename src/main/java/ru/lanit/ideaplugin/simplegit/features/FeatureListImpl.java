package ru.lanit.ideaplugin.simplegit.features;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import cucumber.runtime.CucumberException;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberTagStatement;
import gherkin.parser.Parser;
import gherkin.util.FixJava;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FeatureListImpl extends FeatureList implements BulkFileListener {
    private static final Logger log = Logger.getInstance(FeatureListImpl.class);

    private final Project project;
    private final SimpleGitProjectComponent plugin;
    private List<CucumberFeature> featureList;
    private CucumberFeature selectedFeature;
    private boolean editFeature;

    public FeatureListImpl(Project project) {
        this.project = project;
        this.plugin = project.getComponent(SimpleGitProjectComponent.class);
        //TODO: check feature is dirty on startup
        this.editFeature = false;
        updateFeatures();
        plugin.getProject().getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    private FeatureModel readFeatureFile(String path) throws FileNotFoundException, UnsupportedEncodingException {
        String gherkin = FixJava.readReader(new InputStreamReader(
                new FileInputStream(path), "UTF-8"));
        System.out.println("gherkin...\n" + gherkin);
        FeatureFormatter formatter = new FeatureFormatter();
        Parser parser = new Parser(formatter, false);
        parser.parse(gherkin, path, 0);
        return formatter.getFeatureModel();
    }

    private void updateFeaturesList() {
        List<CucumberFeature> features;
        try {
            features = CucumberFeature.load(
                    new FileResourceLoader(), Collections.singletonList(plugin.getFeaturePath()), Collections.emptyList());
        } catch (CucumberException e) {
            features = new ArrayList<>();
        }
        try {
            readFeatureFile("test.feature");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        for (CucumberFeature feature : features) {
            System.out.println("New feature found at " + feature.getPath());
            System.out.println("  Language: " + feature.getI18n().getIsoCode());
            System.out.println("  Name    : " + feature.getGherkinFeature().getName());
            for (CucumberTagStatement segment : feature.getFeatureElements()) {
                System.out.println("    " + segment.getGherkinModel().getKeyword() + ": " + segment.getGherkinModel().getName());
            }
        }
        this.featureList = features;
    }

    @Override
    public void updateFeatures() {
        updateFeaturesList();
        CucumberFeature select = null;
        if (selectedFeature != null) {
            String path = selectedFeature.getPath();
            select = featureList.stream()
                    .filter(feature -> feature.getPath().equals(path))
                    .findFirst().orElse(null);
        }
        this.selectedFeature = select;
    }

    @Override
    public void updateFeaturesAndSelectByFilename(String filename) {
        updateFeaturesList();
        CucumberFeature select = featureList.stream()
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
    public CucumberFeature getSelectedFeature() {
        return selectedFeature;
    }

    @Override
    public void setSelectedFeature(CucumberFeature selectedFeature) {
        this.selectedFeature = selectedFeature;
        VirtualFile file = plugin.getFeatureDir().findFileByRelativePath(selectedFeature.getPath());
        if (file != null)
            FileEditorManager.getInstance(project).openFile(file, true);
    }

    @Override
    public FeatureState getFeatureState(@NotNull CucumberFeature feature) {
        if (feature == selectedFeature) {
            return editFeature ? FeatureState.EDITED : FeatureState.SELECTED;
        }
        return editFeature ? FeatureState.BLOCKED : FeatureState.SELECTABLE;
    }

    @Override
    public List<CucumberFeature> getFeatureList() {
        return featureList;
    }

    @Override
    public boolean isEnabledFeature(CucumberFeature myFeature) {
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
