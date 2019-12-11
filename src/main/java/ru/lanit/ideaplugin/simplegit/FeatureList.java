package ru.lanit.ideaplugin.simplegit;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.PlatformIcons;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberTagStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FeatureList implements BulkFileListener {
    private static ConcurrentHashMap<Presentation, FeatureList> scenarioListByPresentation = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<JComponent, FeatureList> scenarioListByJComponent = new ConcurrentHashMap<>();

    private static final Icon CHECKED = PlatformIcons.CHECK_ICON;
    private static final Icon EDIT = PlatformIcons.EDIT;

    private SimpleGitPlugin plugin;
    private ConcurrentLinkedQueue<CucumberFeature> items = new ConcurrentLinkedQueue<CucumberFeature>();
    private CucumberFeature selection;
    private Presentation presentation;
    private boolean showDisabledActions;

    public FeatureList(Presentation presentation) {
        System.out.println("Create new FeatureList");
        this.presentation = presentation;
    }

    public static FeatureList getScenarioListFor(Presentation presentation) {
        System.out.println("Try get scenario list by presentation");
        return scenarioListByPresentation.computeIfAbsent(presentation, FeatureList::new);
    }

    public static void registerJComponent(Presentation presentation, JComponent button) {
        System.out.println("Register JComponent");
        FeatureList featureList = scenarioListByPresentation.get(presentation);
        featureList.update();
        scenarioListByJComponent.put(button, featureList);
    }

    void registerPlugin(SimpleGitPlugin plugin) {
        this.plugin = plugin;
        presentation.setEnabled(true);
        updateFeatures();
        plugin.getProject().getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    @NotNull
    public static DefaultActionGroup createPopupActionGroup(JComponent button) {
        FeatureList featureList = scenarioListByJComponent.get(button);
        return featureList.createPopupActionGroup();
    }

    private void setItems(List<CucumberFeature> items, @Nullable CucumberFeature selection) {
        this.items.clear();
        this.items.addAll(items);
        System.out.println("Set items " + items.size());
        setSelection(selection);
    }

    public CucumberFeature getSelection() {
        return selection;
    }

    public void setSelection(CucumberFeature selection) {
        this.selection = selection;
        update();
    }

    public void clearSelection() {
        selection = null;
        update();
    }

    public void showDisabledActions(boolean value) {
        showDisabledActions = value;
    }

    private DefaultActionGroup createPopupActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        System.out.println("Creating popup " + items.size());
        for (final CucumberFeature item : items) {
            if (addSeparator(actionGroup, item)) {
                continue;
            }

            AnAction action = new AnAction() {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    if (selection != item && selectionChanged(item)) {
                        selection = item;
                        FeatureList.this.update(item, presentation, false);
                    }
                }
            };
            actionGroup.add(action);

            Presentation presentation = action.getTemplatePresentation();
            presentation.setIcon(selection == item ? CHECKED : null);
            update(item, presentation, true);
        }

        return actionGroup;
    }


    protected boolean addSeparator(DefaultActionGroup actionGroup, CucumberFeature item) {
        return false;
    }

    public void update() {
        update(selection, presentation, false);
    }

    protected void update(CucumberFeature item, Presentation presentation, boolean popup) {
        presentation.setEnabled(true);
        if (item != null) {
            System.out.println("Updated item " + item.getGherkinFeature().getName());
            if (popup) {
                // For list element
                presentation.setText(item.getGherkinFeature().getName());
            } else {
                // For selected value in ComboBox
                presentation.setText(item.getGherkinFeature().getName());
//                presentation.setIcon(EDIT);
            }
        }
        else {
            // For empty element
//            presentation.setText("[None]");
        }
    }

    public void before(@NotNull List<? extends VFileEvent> events) {
        System.out.println("Updating features");
        this.updateFeatures();
    }

    public void after(@NotNull List<? extends VFileEvent> events) {
        System.out.println("Updating features");
        this.updateFeatures();
    }

    public void updateFeatures() {
        List<CucumberFeature> features = CucumberFeature.load(
                new FileResourceLoader(), Collections.singletonList(plugin.getProject().getBasePath()), Collections.emptyList());
        for (CucumberFeature feature : features) {
            System.out.println("New feature found at " + feature.getPath());
            System.out.println("  Language: " + feature.getI18n().getIsoCode());
            System.out.println("  Name    : " + feature.getGherkinFeature().getName());
            for (CucumberTagStatement segment : feature.getFeatureElements()) {
                System.out.println("    " + segment.getGherkinModel().getKeyword() + ": " + segment.getGherkinModel().getName());
            }
        }

        CucumberFeature select = null;
        if (selection != null) {
            String path = selection.getPath();
            select = features.stream()
                    .filter(feature -> feature.getPath().equals(path))
                    .findFirst().orElse(null);
        }
        setItems(features, select);
    }


    private boolean selectionChanged(CucumberFeature item) {
        System.out.println("New scenario selected: " + item.getPath());
        Project project = plugin.getProject();
        VirtualFile file = project.getBaseDir().findFileByRelativePath(item.getPath());
        FileEditorManager.getInstance(project).openFile(file, true);
        return true;
    }

    public boolean isShowDisabledActions() {
        return showDisabledActions;
    }
}
