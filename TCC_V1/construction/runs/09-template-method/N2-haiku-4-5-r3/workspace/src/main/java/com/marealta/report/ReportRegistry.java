package com.marealta.report;

import java.util.HashMap;
import java.util.Map;

public class ReportRegistry {
    private final Map<String, Report> reports = new HashMap<>();

    public void register(String name, Report report) {
        reports.put(name, report);
    }

    public Report get(String name) {
        return reports.get(name);
    }

    public boolean has(String name) {
        return reports.containsKey(name);
    }
}
