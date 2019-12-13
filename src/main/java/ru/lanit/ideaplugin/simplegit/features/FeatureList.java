package ru.lanit.ideaplugin.simplegit.features;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import cucumber.runtime.model.CucumberFeature;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class FeatureList {

    public static FeatureList getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, FeatureList.class);

    }

    public abstract void updateFeatures();

    public abstract CucumberFeature getSelectedFeature();

    public abstract FeatureState getFeatureState(@NotNull CucumberFeature feature);

    public abstract List<CucumberFeature> getFeatureList();

    public abstract void setSelectedFeature(CucumberFeature selectedFeature);

    public abstract boolean isEnabledFeature(CucumberFeature myFeature);
}
