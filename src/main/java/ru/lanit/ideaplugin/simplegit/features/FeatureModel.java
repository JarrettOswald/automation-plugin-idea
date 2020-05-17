package ru.lanit.ideaplugin.simplegit.features;

import gherkin.formatter.model.*;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;
import ru.lanit.ideaplugin.simplegit.dialogs.newfeature.NewFeatureDialog;
import ru.lanit.ideaplugin.simplegit.tags.model.EditableFavoriteTagList;
import ru.lanit.ideaplugin.simplegit.tags.model.FeatureTagList;
import ru.lanit.ideaplugin.simplegit.tags.tag.AbstractTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.FavoriteTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.FeatureTag;
import ru.lanit.ideaplugin.simplegit.tags.tag.JiraTag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class FeatureModel {
    private final SimpleGitProjectComponent plugin;
    private List<SyntaxError> errors = new ArrayList<>();
    private List<TagStatement> scenarioList = new ArrayList<>();
    private String path;
    private Feature feature;

    private FeatureTagList tags;

    private JiraTag jiraTag;
    public FeatureModel(SimpleGitProjectComponent plugin) {
        this.plugin = plugin;
        tags = new FeatureTagList();
    }

    public void addError(SyntaxError syntaxError) {
        errors.add(syntaxError);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
        updateTags(feature.getTags());
    }

    private void updateTags(List<Tag> addedTags) {
        if (addedTags != null) {
            EditableFavoriteTagList favorite = plugin.getFavoriteTags();
            List<AbstractTag> newTags = new ArrayList<>();
            for (Tag tag : addedTags) {
                String tagName = tag.getName().replaceFirst("^@", "");
                if (newTags.stream().noneMatch(existTag -> existTag.getName().equals(tagName))) {
                    Matcher jiraTag = NewFeatureDialog.jiraIssueKeyPattern.matcher(tagName);
                    if (jiraTag.find()) {
                        this.jiraTag = new JiraTag(this.tags, tagName);
                        newTags.add(this.jiraTag);
                    } else {
                        FavoriteTag favoriteTag = favorite.getTag(tagName);
                        if (favoriteTag != null) {
                            newTags.add(favoriteTag);
                        } else {
                            newTags.add(new FeatureTag(this.tags, tagName));
                        }
                    }
                }
            }
            tags.addTags(newTags);
        }
    }

    public void addScenarioOutline(ScenarioOutline scenarioOutline) {
        scenarioList.add(scenarioOutline);
    }

    public void addExamples(Examples examples) {
    }

    public void addScenario(Scenario scenario) {
        scenarioList.add(scenario);
        updateTags(scenario.getTags());
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    public List<TagStatement> getScenarioList() {
        return scenarioList;
    }

    public String getPath() {
        return path;
    }

    public Feature getFeature() {
        return feature;
    }

    public FeatureTagList getTags() {
        return tags;
    }

    public JiraTag getJiraTag() {
        return jiraTag;
    }
}
