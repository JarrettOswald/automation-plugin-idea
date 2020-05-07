package ru.lanit.ideaplugin.simplegit.features;

import com.intellij.openapi.diagnostic.Logger;
import gherkin.formatter.Formatter;
import gherkin.formatter.model.*;

import java.util.List;

public class FeatureFormatter implements Formatter {
    private static final Logger log = Logger.getInstance(FeatureFormatter.class);

    private final FeatureModel featureModel;

    FeatureFormatter() {
        featureModel = new FeatureModel();
    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
        featureModel.addError(new SyntaxError(state, event, legalEvents, uri, line));
    }

    @Override
    public void uri(String uri) {
        featureModel.setUri(uri);
    }

    @Override
    public void feature(Feature feature) {
        featureModel.setFeature(feature);
    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
        featureModel.addScenarioOutline(scenarioOutline);
    }

    @Override
    public void examples(Examples examples) {
        featureModel.addExamples(examples);
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
    }

    @Override
    public void background(Background background) {
    }

    @Override
    public void scenario(Scenario scenario) {
        featureModel.addScenario(scenario);
    }

    @Override
    public void step(Step step) {
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
    }

    @Override
    public void done() {
    }

    @Override
    public void close() {
    }

    @Override
    public void eof() {
    }

    public FeatureModel getFeatureModel() {
        return featureModel;
    }
}
