package ru.lanit.ideaplugin.simplegit.dialogs.newfeature;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.SizedIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;
import ru.lanit.ideaplugin.simplegit.features.ScenarioType;
import ru.lanit.ideaplugin.simplegit.tags.model.AbstractTagList;
import ru.lanit.ideaplugin.simplegit.tags.model.EditableCommonTagList;
import ru.lanit.ideaplugin.simplegit.tags.model.FixedCommonTagList;
import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.CommonTag;
import ru.lanit.ideaplugin.simplegit.tags.model.FeatureTagList;
import ru.lanit.ideaplugin.simplegit.tags.tag.JiraTag;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NewFeatureDialog extends DialogWrapper {
    private static Pattern jiraIssueKeyPattern = Pattern.compile("^([a-zA-Z][_0-9a-zA-Z]*)-([0-9]+)$");
    private final SimpleGitProjectComponent plugin;
    private JPanel contentPane;
    private JTextField scenarioName;
    private JComboBox<String> featureName;
    private JTextField featureFilename;
    private JButton addNewTag;
    private JButton removeTag;
    private JButton getCommonTag;
    private JComboBox<ScenarioType> scenarioType;
    private JTable featureTagsTable;
    private JTextField jiraIssueKey;
    private JTable commonTagsTable;
    private FeatureTagList featureTagsList = new FeatureTagList();
    private FixedCommonTagList commonTagsList;

    public NewFeatureDialog(@Nullable Project project) {
        super(project);
        this.plugin = project.getComponent(SimpleGitProjectComponent.class);
        addNewTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.Add, 16, 16)));
        addNewTag.addActionListener(this::addNewTagAction);
        removeTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.Remove, 16, 16)));
        removeTag.addActionListener(this::removeTagAction);
        getCommonTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.SplitLeft, 16, 16)));
        getCommonTag.addActionListener(this::getCommonTagAction);
        scenarioType.setModel(new DefaultComboBoxModel<>(ScenarioType.values()));

        DocumentListener featureNameDocumentListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {rebuildFeatureFilename();}
            @Override public void removeUpdate(DocumentEvent e) {rebuildFeatureFilename();}
            @Override public void changedUpdate(DocumentEvent e) {rebuildFeatureFilename();}
        };
        final JTextComponent tc = (JTextComponent) featureName.getEditor().getEditorComponent();
        tc.getDocument().addDocumentListener(featureNameDocumentListener);
        scenarioName.getDocument().addDocumentListener(featureNameDocumentListener);

        DocumentListener jiraTaskDocumentListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {rebuildJiraIssueKeyTag();}
            @Override public void removeUpdate(DocumentEvent e) {rebuildJiraIssueKeyTag();}
            @Override public void changedUpdate(DocumentEvent e) {rebuildJiraIssueKeyTag();}
        };
        UpperCaseDocument ucd = new UpperCaseDocument();
        ucd.addDocumentListener(jiraTaskDocumentListener);
        ucd.setUpperCase(true);
        jiraIssueKey.setDocument(ucd);

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
        AbstractTagList.prepareTable(featureTagsTable);
        featureTagsList.attachToTable(featureTagsTable);
        AbstractTagList.prepareTable(commonTagsTable);
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

    private void rebuildJiraIssueKeyTag() {
        Optional<JiraTag> tag = featureTagsList.getTags().stream()
                .filter(JiraTag.class::isInstance)
                .map(JiraTag.class::cast)
                .findFirst();
        String jiraTagName = jiraIssueKey.getText();
        if (jiraTagName.isEmpty()) {
            tag.ifPresent(jiraTag -> featureTagsList.removeTag(jiraTag.getIndex()));
        } else {
            JiraTag jtag = tag.orElseGet(() -> {
                JiraTag jiraTag = new JiraTag(featureTagsList);
                featureTagsList.insertTag(0, jiraTag);
                return jiraTag;
            });
            jtag.setName(jiraTagName);
            featureTagsList.fireTableCellUpdated(jtag.getIndex(), 0);
        }
    }

    private void rebuildFeatureFilename() {
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
        List<AbstractTag> removedTags = featureTagsList.removeTags(featureTagsTable.getSelectedRows());
        List<CommonTag> commonTags = removedTags.stream()
                .filter(CommonTag.class::isInstance).map(CommonTag.class::cast)
                .collect(Collectors.toList());
        commonTagsList.addTags(commonTags);
    }

    private void getCommonTagAction(ActionEvent e) {
        List<CommonTag> removedTags = commonTagsList.removeTags(commonTagsTable.getSelectedRows());
        featureTagsList.addTags(removedTags);
        updateSelection();
    }

    private void updateSelection() {
        if (featureTagsList.getRowCount() > 0 && featureTagsTable.getSelectedRows().length == 0) {
            featureTagsTable.addRowSelectionInterval(0, 0);
        }
        if (commonTagsList.getRowCount() > 0 && commonTagsTable.getSelectedRows().length == 0) {
            commonTagsTable.addRowSelectionInterval(0, 0);
        }
    }

    public void setCommonTags(EditableCommonTagList commonTagsList) {
        this.commonTagsList = new FixedCommonTagList(commonTagsList);
        this.commonTagsList.attachToTable(commonTagsTable);
    }

    public List<AbstractTag> getFeatureTags() {
        return featureTagsList.getTags();
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

    public ScenarioType getScenarioType() {
        return (ScenarioType) scenarioType.getSelectedItem();
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
        Matcher result = jiraIssueKeyPattern.matcher(jiraIssueKey.getText());
        if (!result.find() || result.group(2) == null) {
            return new ValidationInfo("Jira Issue Key must be of the format &lt;PROJECT_NAME&gt;-&lt;ISSUE_NUMBER&gt;", jiraIssueKey);
        }
        return null;
    }

    private static class UpperCaseDocument extends PlainDocument {
        private boolean upperCase = true;

        public void setUpperCase(boolean flag) {
            upperCase = flag;
        }

        public void insertString(int offset, String str, AttributeSet attSet)
                throws BadLocationException {
            if (upperCase)
                str = str.toUpperCase();
            super.insertString(offset, str, attSet);
        }
    }
}