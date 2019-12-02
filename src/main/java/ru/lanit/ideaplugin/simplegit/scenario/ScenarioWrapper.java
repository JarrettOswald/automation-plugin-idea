package ru.lanit.ideaplugin.simplegit.scenario;

import java.io.File;

public class ScenarioWrapper {
    private String scenarioName;
    private String fileName;
    private Boolean dirty;

    public ScenarioWrapper(String fileName) {
        this.fileName = fileName;
        this.scenarioName = new File(fileName).getName();
        this.dirty = false;
    }

    public ScenarioWrapper() {
        this.fileName = null;
        this.scenarioName = null;
        this.dirty = true;
    }

    public String getScenarioName() {
        return scenarioName;
    }
}
