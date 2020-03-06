package ru.lanit.ideaplugin.simplegit.features;

public enum ScenarioType {
    SCENARIO("Сценарий"),
    SCENARIO_OUTLINE("Структура сценария");

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
}
