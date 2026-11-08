package pousada.report;

import java.time.LocalDate;

/** O intervalo [from, toExclusive) que um relatório cobre, e como ele aparece no título. */
public record Period(String label, LocalDate from, LocalDate toExclusive) {
}
