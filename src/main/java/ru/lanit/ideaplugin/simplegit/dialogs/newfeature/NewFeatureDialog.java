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
import ru.lanit.ideaplugin.simplegit.tags.model.EditableCommonTagList;
import ru.lanit.ideaplugin.simplegit.tags.model.FixedCommonTagList;
import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.CommonTag;
import ru.lanit.ideaplugin.simplegit.tags.model.FeatureTagList;

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
import java.util.function.Predicate;
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
    private JList<CommonTag> commonTags;
    private JComboBox scenarioType;
    private JTable featureTagsTable;
    private JTextField jiraTask;
    private DefaultListModel<CommonTag> commonTagsModel = new DefaultListModel<>();
    private DefaultListModel<AbstractTag> featureTagsModel = new DefaultListModel<>();
    private FeatureTagList featureTagsList = new FeatureTagList();

    public NewFeatureDialog(@Nullable Project project) {
        super(project);
        this.plugin = project.getComponent(SimpleGitProjectComponent.class);
        addNewTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.Add, 16, 16)));
        addNewTag.addActionListener(this::addNewTagAction);
        removeTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.Remove, 16, 16)));
        removeTag.addActionListener(this::removeTagAction);
        getCommonTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.SplitLeft, 16, 16)));
        getCommonTag.addActionListener(this::getCommonTagAction);

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
//        featureTags.setModel(featureTagsModel);
        featureTagsList.attachToTable(featureTagsTable);
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

    private void addNewTagAction(ActionEvent e) {
        featureTagsList.addNewTag();
    }

    private void removeTagAction(ActionEvent e) {
//        CommonTag[] tags = (CommonTag[]) commonTagsModel.toArray();
//        commonTagsModel.clear();
//        Stream.concat(
//                Arrays.stream(tags),
//                featureTags.getSelectedValuesList().stream().filter(CommonTag.class::isInstance).map(CommonTag.class::cast)
//        ).sorted(Comparator.comparingInt(AbstractTag::getIndex)).forEachOrdered(commonTagsModel::addElement);
//        int[] selectedIndices = featureTags.getSelectedIndices();
//        for (int i = selectedIndices.length - 1; i >= 0; i--) {
//            featureTagsModel.remove(selectedIndices[i]);
//        }
//        updateSelection();
    }

    private void getCommonTagAction(ActionEvent e) {
        featureTagsList.addTags(commonTags.getSelectedValuesList());
        /*featureTagsModel.clear();
        Stream.concat(
                Arrays.stream(tags).map(obj -> (AbstractTag) obj).filter(CommonTag.class::isInstance),
                commonTags.getSelectedValuesList().stream()
        ).sorted(Comparator.comparingInt(AbstractTag::getIndex)).forEachOrdered(tag -> {
            featureTagsModel.addElement(tag);
            featureTagsList.addNewTag(tag);
        });
        Arrays.stream(tags).map(obj -> (AbstractTag) obj)
                .filter(((Predicate<AbstractTag>) CommonTag.class::isInstance).negate()).forEachOrdered(tag -> {
            featureTagsModel.addElement(tag);
            featureTagsList.addNewTag(tag);
        });*/
        int[] selectedIndices = commonTags.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            commonTagsModel.remove(selectedIndices[i]);
        }
        updateSelection();
    }

    private void updateSelection() {
        if (featureTagsList.getRowCount() > 0 && featureTagsTable.getSelectedRows().length == 0) {
            featureTagsTable.addRowSelectionInterval(0, 0);
        }
        if (commonTagsModel.size() > 0 && commonTags.getSelectedValue() == null) {
            commonTags.setSelectedIndex(0);
        }
    }

    public void setCommonTags(EditableCommonTagList commonTagsList) {
        commonTagsModel.clear();
        if (commonTagsList != null) {
            commonTagsList.getTags().forEach(commonTagsModel::addElement);
            commonTags.setSelectedIndex(0);
        }
    }

    public List<Tag> getFeatureTags() {
        Object[] tags = featureTagsModel.toArray();
        List<Tag> result = new ArrayList<>();
        for(int i = 0; i < tags.length; i++) {
            result.add(new Tag(((AbstractTag) tags[i]).getName(), i));
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