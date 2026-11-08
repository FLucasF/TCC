package br.com.marealta.report;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Tourism tax charged by the city, per guest per night. The city changes it every year. */
public interface TourismTax {
    BigDecimal perGuestNight(YearMonth month);

    static TourismTax flat(BigDecimal amount) {
        return month -> amount;
    }
}
