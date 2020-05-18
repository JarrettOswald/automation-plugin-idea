package ru.lanit.ideaplugin.simplegit.features;

import java.util.List;

public class SyntaxError {
    private final String state;
    private final String event;
    private final List<String> legalEvents;
    private final String uri;

    public SyntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
        this.state = state;
        this.event = event;
        this.legalEvents = legalEvents;
        this.uri = uri;
        this.line = line;
    }

    private final Integer line;

    public String getState() {
        return state;
    }

    public String getEvent() {
        return event;
    }

    public List<String> getLegalEvents() {
        return legalEvents;
    }

    public String getUri() {
        return uri;
    }

    public Integer getLine() {
        return line;
    }
}
