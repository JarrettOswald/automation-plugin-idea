package ru.lanit.ideaplugin.simplegit.dialogs.newfeature;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.SizedIcon;
import com.intellij.util.ui.JBUI;
import gherkin.formatter.model.BasicStatement;
import gherkin.formatter.model.TagStatement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;
import ru.lanit.ideaplugin.simplegit.features.FeatureList;
import ru.lanit.ideaplugin.simplegit.features.FeatureModel;
import ru.lanit.ideaplugin.simplegit.features.ScenarioType;
import ru.lanit.ideaplugin.simplegit.tags.model.AbstractTagList;
import ru.lanit.ideaplugin.simplegit.tags.model.EditableFavoriteTagList;
import ru.lanit.ideaplugin.simplegit.tags.model.FixedFavoriteTagList;
import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.FavoriteTag;
import ru.lanit.ideaplugin.simplegit.tags.model.FeatureTagList;
import ru.lanit.ideaplugin.simplegit.tags.tag.FeatureTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.JiraTag;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.lanit.ideaplugin.simplegit.localization.Language.simpleGitPluginBundle;

public class NewFeatureDialog extends DialogWrapper {
    private static final Logger log = Logger.getInstance(NewFeatureDialog.class);

    public static Pattern jiraIssueKeyPattern = Pattern.compile("^([a-zA-Z][_0-9a-zA-Z]*)-([0-9]+)$");
    private final SimpleGitProjectComponent plugin;
    private JPanel contentPane;
    private JTextField scenarioName;
    private JComboBox<String> featureName;
    private JTextField featureFilename;
    private JButton addNewTag;
    private JButton removeTag;
    private JButton getFavoriteTag;
    private JComboBox<ScenarioType> scenarioType;
    private JTable featureTagsTable;
    private JTextField jiraIssueKey;
    private JTable favoriteTagsTable;
    private JLabel featureNameLabel;
    private JLabel scenarioNameLabel;
    private JLabel scenarioTypeLabel;
    private JLabel jiraIssueKeyLabel;
    private JLabel featureFilenameLabel;
    private JLabel scenarioTagsLabel;
    private JLabel favoriteTagsLabel;
    private JCheckBox isCopyFromScenario;
    private JComboBox<String> copyFromScenario;
    private FeatureTagList featureTagsList = new FeatureTagList();
    private FixedFavoriteTagList favoriteTagsList;

    public NewFeatureDialog(@Nullable Project project) {
        super(project);
        this.plugin = project.getComponent(SimpleGitProjectComponent.class);
        isCopyFromScenario.setText(simpleGitPluginBundle.getString("create-new-scenario.dialog.copy-from-scenario"));
        featureNameLabel.setText(simpleGitPluginBundle.getString("create-new-scenario.dialog.feature.name"));
        scenarioNameLabel.setText(simpleGitPluginBundle.getString("create-new-scenario.dialog.scenario.name"));
        scenarioTypeLabel.setText(simpleGitPluginBundle.getString("create-new-scenario.dialog.scenario.type"));
        jiraIssueKeyLabel.setText(simpleGitPluginBundle.getString("create-new-scenario.dialog.jira-issue-key"));
        featureFilenameLabel.setText(simpleGitPluginBundle.getString("create-new-scenario.dialog.feature.filename"));
        scenarioTagsLabel.setText(simpleGitPluginBundle.getString("create-new-scenario.dialog.tags.scenario"));
        favoriteTagsLabel.setText(simpleGitPluginBundle.getString("create-new-scenario.dialog.tags.favorite"));

        addNewTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.Add, 16, 16)));
        addNewTag.addActionListener(this::addNewTagAction);
        removeTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.Remove, 16, 16)));
        removeTag.addActionListener(this::removeTagAction);

        BuildNumber requiredBuild = BuildNumber.fromString("183.2407.10");
        if (requiredBuild != null && SimpleGitProjectComponent.applicationInfo.getBuild().compareTo(requiredBuild) >= 0) {
            Icon ArrowLeft = IconLoader.getIcon("/general/arrowLeft.svg");
            getFavoriteTag.setIcon(JBUI.scale(new SizedIcon(ArrowLeft, 16, 16)));
        } else {
            getFavoriteTag.setIcon(JBUI.scale(new SizedIcon(AllIcons.General.SplitLeft, 16, 16)));
        }
        getFavoriteTag.addActionListener(this::getFavoriteTagAction);
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

        isCopyFromScenario.addChangeListener(this::updateCopyFromScenario);

        setModal(true);
        pack();
        validate();
        init();
        setTitle(simpleGitPluginBundle.getString("create-new-scenario.dialog.title"));
    }

    boolean isOkEnabled() {
        // return true if dialog can be closed
        return true;
    }

    private void updateCopyFromScenario(ChangeEvent changeEvent) {
        boolean enabled = isCopyFromScenario.isSelected();
        copyFromScenario.setEnabled(enabled);
        if (enabled) {
            FeatureList featureList = FeatureList.getInstance(plugin.getProject());
            List<FeatureModel> features = featureList.getFeatureList();
            String current = (String) copyFromScenario.getSelectedItem();
            if (current == null) {
                FeatureModel feature = featureList.getSelectedFeature();
                if (feature != null) {
                    current = feature.getScenarioList().stream()
                            .map(BasicStatement::getName)
                            .findFirst()
                            .orElse(feature.getFeature().getName());
                }
            }
            copyFromScenario.removeAllItems();
            FeatureModel selectedCopyFrom = null;
            Integer selected = null;
            for (FeatureModel feature : features) {
                String item = feature.getScenarioList().stream()
                        .map(BasicStatement::getName)
                        .findFirst()
                        .orElse(feature.getFeature().getName());
                copyFromScenario.addItem(item);
                if (item.equals(current)) {
                    selected = copyFromScenario.getItemCount() - 1;
                }
            }
            if (selected != null) {
                copyFromScenario.setSelectedIndex(selected);
            } else {
                copyFromScenario.setSelectedIndex(-1);
            }
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        AbstractTagList.prepareTable(featureTagsTable);
        featureTagsList.attachToTable(featureTagsTable);
        AbstractTagList.prepareTable(favoriteTagsTable);
        copyFromScenario.addActionListener(this::onSelectCopyFromScenario);
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

    private void onSelectCopyFromScenario(ActionEvent actionEvent) {
        FeatureModel selected = getCopyFromScenario();
        if (selected != null) {
            featureName.getEditor().setItem(selected.getFeature().getName());
            List<TagStatement> scenarios = selected.getScenarioList();
            if (scenarios.size() > 0) {
                TagStatement scenario = scenarios.get(0);
                scenarioName.setText(scenario.getName());
                ScenarioType type = ScenarioType.getByName(scenario.getKeyword());
                if (type != null) {
                    scenarioType.setSelectedItem(type);
                }
            }
            restoreFavorites();
            featureTagsList.clear();
            List<AbstractTag> tags = selected.getTags().getTags();
            List<AbstractTag> featureTags = tags.stream().filter(FeatureTag.class::isInstance).collect(Collectors.toList());
            featureTagsList.addTags(featureTags);
            List<FavoriteTag> favoriteTags = favoriteTagsList.getTagsByName(
                    tags.stream()
                            .filter(FavoriteTag.class::isInstance).map(FavoriteTag.class::cast)
                            .collect(Collectors.toList())
            );
            favoriteTagsList.removeTags(favoriteTags);
            featureTagsList.addTags(favoriteTags);
        }
    }

    public FeatureModel getCopyFromScenario() {
        if (isCopyFromScenario.isSelected()) {
            int selected = copyFromScenario.getSelectedIndex();
            if (selected >= 0) {
                String current = copyFromScenario.getItemAt(selected);
                FeatureList featureList = FeatureList.getInstance(plugin.getProject());
                List<FeatureModel> features = featureList.getFeatureList();
                for (FeatureModel feature : features) {
                    String item = feature.getScenarioList().stream()
                            .map(BasicStatement::getName)
                            .findFirst()
                            .orElse(feature.getFeature().getName());
                    if (item.equals(current)) {
                        return feature;
                    }
                }
            }
        }
        return null;
    }

    private void rebuildFeatureFilename() {
        @NonNls String filename;
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
        List<FavoriteTag> favoriteTags = removedTags.stream()
                .filter(FavoriteTag.class::isInstance).map(FavoriteTag.class::cast)
                .collect(Collectors.toList());
        favoriteTagsList.addTags(favoriteTags);
    }

    private void getFavoriteTagAction(ActionEvent e) {
        List<FavoriteTag> removedTags = favoriteTagsList.removeTags(favoriteTagsTable.getSelectedRows());
        featureTagsList.addTags(removedTags);
        updateSelection();
    }

    private void updateSelection() {
        if (featureTagsList.getRowCount() > 0 && featureTagsTable.getSelectedRows().length == 0) {
            featureTagsTable.addRowSelectionInterval(0, 0);
        }
        if (favoriteTagsList.getRowCount() > 0 && favoriteTagsTable.getSelectedRows().length == 0) {
            favoriteTagsTable.addRowSelectionInterval(0, 0);
        }
    }

    private void restoreFavorites() {
        List<FavoriteTag> favoriteTags = featureTagsList.getTags().stream()
                .filter(FavoriteTag.class::isInstance).map(FavoriteTag.class::cast)
                .collect(Collectors.toList());
        favoriteTagsList.addTags(favoriteTags);
    }

    public void setFavoriteTags(EditableFavoriteTagList favoriteTagsList) {
        this.favoriteTagsList = new FixedFavoriteTagList(favoriteTagsList);
        this.favoriteTagsList.attachToTable(favoriteTagsTable);
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
        if (isCopyFromScenario.isSelected() && copyFromScenario.getSelectedIndex() == -1) {
            return new ValidationInfo(simpleGitPluginBundle.getString("create-new-scenario.dialog.validate-error.copy-from-scenario"), featureName);
        }
        if (getFeatureName().isEmpty()) {
            return new ValidationInfo(simpleGitPluginBundle.getString("create-new-scenario.dialog.validate-error.name.feature"), featureName);
        }
        if (getScenarioName().isEmpty()) {
            return new ValidationInfo(simpleGitPluginBundle.getString("create-new-scenario.dialog.validate-error.name.scenario"), scenarioName);
        }
        if (getFeatureFilename().isEmpty() || getFeatureFilename().endsWith("\\")) {
            return new ValidationInfo(simpleGitPluginBundle.getString("create-new-scenario.dialog.validate-error.feature-filename.pickup"), featureFilename);
        }
        File file = new File(plugin.getFeaturePath(), getFeatureFilename());
        if (file.exists()) {
            return new ValidationInfo(simpleGitPluginBundle.getString("create-new-scenario.dialog.validate-error.feature-filename.exists"), featureFilename);
        }
        Matcher result = jiraIssueKeyPattern.matcher(jiraIssueKey.getText());
        if (!result.find() || result.group(2) == null) {
            return new ValidationInfo(simpleGitPluginBundle.getString("create-new-scenario.dialog.validate-error.jira-issue-key.format"), jiraIssueKey);
        }
        return null;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        copyFromScenario = new JComboBox<>();
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