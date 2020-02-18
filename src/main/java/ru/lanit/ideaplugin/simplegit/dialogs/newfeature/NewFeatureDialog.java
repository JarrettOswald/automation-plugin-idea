package ru.lanit.ideaplugin.simplegit.dialogs.newfeature;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.SizedIcon;
import com.intellij.util.ui.JBUI;
import gherkin.formatter.model.Tag;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;
import ru.lanit.ideaplugin.simplegit.FeatureTag;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class NewFeatureDialog extends DialogWrapper {
    private final SimpleGitProjectComponent plugin;
    private JPanel contentPane;
    private JTextField scenarioName;
    private JComboBox<String> featureName;
    private JTextField featureFilename;
    private JButton addNewTag;
    private JButton removeTag;
    private JButton getCommonTag;
    private JList<FeatureTag> featureTags;
    private JList<FeatureTag> commonTags;
    private JScrollPane featureTagsScrollPane;
    private JPanel toolPanel;
    private JScrollPane commonTagsScrollPane;
    private JComboBox comboBox1;
    private DefaultListModel<FeatureTag> commonTagsModel = new DefaultListModel<>();
    private DefaultListModel<FeatureTag> featureTagsModel = new DefaultListModel<>();

    public NewFeatureDialog(@Nullable Project project) {
        super(project);
        this.plugin = project.getComponent(SimpleGitProjectComponent.class);
        addNewTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.Add, 16, 16)));
        addNewTag.addActionListener(this::addNewTagListener);
        removeTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.Remove, 16, 16)));
        removeTag.addActionListener(this::removeTagListener);
        getCommonTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.SplitLeft, 16, 16)));
        getCommonTag.addActionListener(this::getCommonTagListener);

        DocumentListener featureNameDocumentListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {rebuildFeatureFilename(null);}
            @Override public void removeUpdate(DocumentEvent e) {rebuildFeatureFilename(null);}
            @Override public void changedUpdate(DocumentEvent e) {rebuildFeatureFilename(null);}
        };
        final JTextComponent tc = (JTextComponent) featureName.getEditor().getEditorComponent();
        tc.getDocument().addDocumentListener(featureNameDocumentListener);
        scenarioName.getDocument().addDocumentListener(featureNameDocumentListener);
        setModal(true);
        pack();
        validate();
        init();
        setTitle("Create new scenario");
    }

    boolean isOkEnabled() {
        // return true if dialog can be closed
        return true;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        commonTags.setModel(commonTagsModel);
        featureTags.setModel(featureTagsModel);
        featureTags.setCellRenderer(FeatureTag.getCellRenderer());
        return contentPane;
    }

    private String escapeFilename(String name) {
        String result = name.replaceAll("[<>:\"\\\\/|?]", "_");
        result = result.replaceAll("(^(\\.|\\s)+|(\\.|\\s)+$)", "");
        if (result.matches("^(?i)(con|prn|aux|nul|com\\d|lpt\\d)$")) {
            return "";
        }
        return result;
    }

    private void rebuildFeatureFilename(ActionEvent actionEvent) {
        String filename;
        String dir = escapeFilename(getFeatureName());
        String fn = escapeFilename(getScenarioName());
        if (dir.isEmpty()) {
            filename = "";
        } else {
            filename = dir +  "\\";
        }
        if (!fn.isEmpty()) {
            filename = filename + fn + ".feature";
        }
        featureFilename.setText(filename);
    }

    private void addNewTagListener(ActionEvent e) {

    }

    private void removeTagListener(ActionEvent e) {
        Object[] tags = commonTagsModel.toArray();
        commonTagsModel.clear();
        Stream.concat(
                Arrays.stream(tags).map(obj -> (FeatureTag) obj),
                featureTags.getSelectedValuesList().stream().filter(FeatureTag::isCommon)
        ).sorted(Comparator.comparingInt(FeatureTag::getIndex)).forEachOrdered(commonTagsModel::addElement);
        int[] selectedIndices = featureTags.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            featureTagsModel.remove(selectedIndices[i]);
        }
        updateSelection();
    }

    private void getCommonTagListener(ActionEvent e) {
        Object[] tags = featureTagsModel.toArray();
        featureTagsModel.clear();
        Stream.concat(
                Arrays.stream(tags).map(obj -> (FeatureTag) obj).filter(FeatureTag::isCommon),
                commonTags.getSelectedValuesList().stream()
        ).sorted(Comparator.comparingInt(FeatureTag::getIndex)).forEachOrdered(featureTagsModel::addElement);
        Arrays.stream(tags).map(obj -> (FeatureTag) obj)
                .filter(tag -> !tag.isCommon()).forEachOrdered(featureTagsModel::addElement);
        int[] selectedIndices = commonTags.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            commonTagsModel.remove(selectedIndices[i]);
        }
        updateSelection();
    }

    private void updateSelection() {
        if (featureTagsModel.size() > 0 && featureTags.getSelectedValue() == null) {
            featureTags.setSelectedIndex(0);
        }
        if (commonTagsModel.size() > 0 && commonTags.getSelectedValue() == null) {
            commonTags.setSelectedIndex(0);
        }
    }

    public void setCommonTags(List<String> commonTagsList) {
        commonTagsModel.clear();
        if (commonTagsList != null) {
            for (int i = 0; i < commonTagsList.size(); i++) {
                FeatureTag tag = new FeatureTag(commonTagsList.get(i), i);
                commonTagsModel.addElement(tag);
            }
            commonTags.setSelectedIndex(0);
        }
    }

    public List<Tag> getFeatureTags() {
        Object[] tags = featureTagsModel.toArray();
        List<Tag> result = new ArrayList<>();
        for(int i = 0; i < tags.length; i++) {
            result.add(new Tag(((FeatureTag) tags[i]).getName(), i));
        }
        return result;
    }

    public String getFeatureName() {
        return (String) featureName.getEditor().getItem();
    }

    public String getFeatureFilename() {
        return featureFilename.getText();
    }

    public String getScenarioName() {
        return scenarioName.getText();
    }

    @Override
    public ValidationInfo doValidate() {
        if (getFeatureName().isEmpty()) {
            return new ValidationInfo("Need to set Feature Name", featureName);
        }
        if (getScenarioName().isEmpty()) {
            return new ValidationInfo("Need to set Scenario Name", scenarioName);
        }
        if (getFeatureFilename().isEmpty() || getFeatureFilename().endsWith("\\")) {
            return new ValidationInfo("Cannnot pick up suitable Feature Filename", featureFilename);
        }
        File file = new File(plugin.getFeaturePath(), getFeatureFilename());
        if (file.exists()) {
            return new ValidationInfo("Feature file with this Filename already exists", featureFilename);
        }
        return null;
    }
}