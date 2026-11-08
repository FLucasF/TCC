package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Taxa de turismo por hóspede por noite. A prefeitura muda o valor todo ano:
 * basta acrescentar uma vigência (data de início, valor) em {@link #standard()}.
 */
public final class TourismTax {
    private final NavigableMap<LocalDate, BigDecimal> ratesByStart = new TreeMap<>();

    public TourismTax(Map<LocalDate, BigDecimal> ratesByStart) {
        if (ratesByStart.isEmpty()) {
            throw new IllegalArgumentException("É preciso ao menos uma vigência");
        }
        this.ratesByStart.putAll(ratesByStart);
    }

    public static TourismTax standard() {
        return new TourismTax(Map.of(LocalDate.MIN, new BigDecimal("4.50")));
    }

    public BigDecimal ratePerGuestNight(LocalDate date) {
        Map.Entry<LocalDate, BigDecimal> entry = ratesByStart.floorEntry(date);
        return entry != null ? entry.getValue() : ratesByStart.firstEntry().getValue();
    }

    /** Taxa de uma reserva, pela vigência da data de entrada. */
    public BigDecimal forReservation(Reservation r) {
        return Money.round(ratePerGuestNight(r.checkIn()).multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
