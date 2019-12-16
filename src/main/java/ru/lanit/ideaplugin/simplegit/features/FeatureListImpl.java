package ru.lanit.ideaplugin.simplegit.features;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberTagStatement;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

import java.util.Collections;
import java.util.List;

public class FeatureListImpl extends FeatureList implements BulkFileListener {

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

    @Override
    public void updateFeatures() {
        List<CucumberFeature> features = CucumberFeature.load(
                new FileResourceLoader(), Collections.singletonList(plugin.getFeaturePath()), Collections.emptyList());
        for (CucumberFeature feature : features) {
            System.out.println("New feature found at " + feature.getPath());
            System.out.println("  Language: " + feature.getI18n().getIsoCode());
            System.out.println("  Name    : " + feature.getGherkinFeature().getName());
            for (CucumberTagStatement segment : feature.getFeatureElements()) {
                System.out.println("    " + segment.getGherkinModel().getKeyword() + ": " + segment.getGherkinModel().getName());
            }
        }

        CucumberFeature select = null;
        if (selectedFeature != null) {
            String path = selectedFeature.getPath();
            select = features.stream()
                    .filter(feature -> feature.getPath().equals(path))
                    .findFirst().orElse(null);
        }
        this.featureList = features;
        this.selectedFeature = select;
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