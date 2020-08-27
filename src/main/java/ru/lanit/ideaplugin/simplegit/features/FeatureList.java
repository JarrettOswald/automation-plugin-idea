package ru.lanit.ideaplugin.simplegit.features;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class FeatureList {

    public static FeatureList getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, FeatureList.class);

    }

    public abstract void updateFeatures(AnActionEvent event);

    public abstract void updateFeaturesAndSelectByFile(VirtualFile file, AnActionEvent event);

    public abstract FeatureModel getSelectedFeature();

    public abstract FeatureState getFeatureState(@NotNull FeatureModel feature);

    public abstract List<FeatureModel> getFeatureList();

    public abstract void setSelectedFeature(FeatureModel selectedFeature, AnActionEvent event);

    public abstract boolean isEnabledFeature(FeatureModel myFeature);
}
