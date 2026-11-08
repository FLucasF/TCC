package com.marealta.report;

import java.time.LocalDate;

public interface Report {
    String generate(LocalDate parameter);
}
