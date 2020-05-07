package ru.lanit.ideaplugin.simplegit.features;

import gherkin.formatter.model.*;

import java.util.ArrayList;
import java.util.List;

public class FeatureModel {
    private List<SyntaxError> errors = new ArrayList<>();
    private List<TagStatement> scenarioList = new ArrayList<>();
    private String uri;
    private Feature feature;

    public FeatureModel() {
    }

    public void addError(SyntaxError syntaxError) {
        errors.add(syntaxError);
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public void addScenarioOutline(ScenarioOutline scenarioOutline) {
        scenarioList.add(scenarioOutline);
    }

    public void addExamples(Examples examples) {
    }

    public void addScenario(Scenario scenario) {
        scenarioList.add(scenario);
    }
}
