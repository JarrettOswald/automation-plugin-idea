package ru.lanit.ideaplugin.simplegit.features;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SizedIcon;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberTagStatement;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

import javax.swing.*;
import java.util.*;

public class FeatureListManager {
    private static final Icon CHECKED = PlatformIcons.CHECK_ICON;
    private static final Icon EDIT = PlatformIcons.EDIT;

//    public static final Icon CHECKED_ICON = JBUI.scale(new SizedIcon(AllIcons.Actions.Checked, 16, 16));
//    public static final Icon CHECKED_SELECTED_ICON = JBUI.scale(new SizedIcon(AllIcons.Actions.Checked_selected, 16, 16));
    public static final Icon EMPTY_ICON = EmptyIcon.ICON_16;

    private final Project project;
    private final SimpleGitPlugin plugin;
    private List<CucumberFeature> featureList;
    private CucumberFeature selectedFeature;
    private boolean editFeature;

    public FeatureListManager(Project project) {
        this.project = project;
        this.plugin = SimpleGitPlugin.getPluginFor(project);
        //TODO: check feature is dirty on startup
        this.editFeature = false;
        updateFeatures();
    }

    private void updateFeatures() {
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

    public static FeatureListManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, FeatureListManager.class);
    }

    public CucumberFeature getSelectedFeature() {
        return selectedFeature;
    }

    public Icon getFeatureIcon(@NotNull CucumberFeature feature) {
        if (feature == selectedFeature) {
            return editFeature ? EDIT : CHECKED;
        }
        return EMPTY_ICON;
    }

    public List<CucumberFeature> getFeatureList() {
        return featureList;
    }

    public void setSelectedFeature(CucumberFeature selectedFeature) {
        this.selectedFeature = selectedFeature;
    }

    public boolean isEnabled(CucumberFeature myFeature) {
        return myFeature == selectedFeature || !editFeature;
    }
}
