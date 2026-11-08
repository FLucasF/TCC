package com.marealta.events;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

final class Dates {
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Dates() {
    }

    static String br(LocalDate date) {
        return date.format(BR);
    }
}
