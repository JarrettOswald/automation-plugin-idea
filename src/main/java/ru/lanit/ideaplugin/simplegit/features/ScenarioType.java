package ru.lanit.ideaplugin.simplegit.features;

import static ru.lanit.ideaplugin.simplegit.localization.Language.simpleGitPluginBundle;

public enum ScenarioType {
    SCENARIO(simpleGitPluginBundle.getString("scenario.type.scenario")),
    SCENARIO_OUTLINE(simpleGitPluginBundle.getString("scenario.type.scenario-outline"));

    private final String name;

    ScenarioType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static ScenarioType getByName(String name) {
        for (ScenarioType scenarioType : values()) {
            if (scenarioType.name.equalsIgnoreCase(name)) {
                return scenarioType;
            }
        }
        return null;
    }
}
