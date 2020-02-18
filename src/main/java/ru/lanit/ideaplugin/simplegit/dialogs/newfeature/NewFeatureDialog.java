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
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class NewFeatureDialog extends DialogWrapper {
    private final SimpleGitProjectComponent plugin;
    private JPanel contentPane;
    private JTextField scenarioName;
    private JTextField featureName;
    private JTextField featureFilename;
    private JButton addNewTag;
    private JButton removeTag;
    private JButton getCommonTag;
    private JList<FeatureTag> featureTags;
    private JList<FeatureTag> commonTags;
    private JScrollPane featureTagsScrollPane;
    private JPanel toolPanel;
    private JScrollPane commonTagsScrollPane;
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
        return featureName.getText();
    }

    public String getFeatureFilename() {
        return featureFilename.getText();
    }

    public String getScenarioName() {
        return scenarioName.getText();
    }

    @Override
    public ValidationInfo doValidate() {
        if (featureName.getText().isEmpty()) {
            return new ValidationInfo("Need to set Feature Name", featureName);
        }
        if (scenarioName.getText().isEmpty()) {
            return new ValidationInfo("Need to set Scenario Name", scenarioName);
        }
        if (featureFilename.getText().isEmpty()) {
            return new ValidationInfo("Need to set Feature Filename", featureFilename);
        }
        return null;
    }
}