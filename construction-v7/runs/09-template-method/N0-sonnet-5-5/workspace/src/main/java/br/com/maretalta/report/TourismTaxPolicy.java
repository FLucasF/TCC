package br.com.maretalta.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Municipal tax per guest per night; the city changes it yearly, so it is looked up by the stay's check-in year. */
public class TourismTaxPolicy {
    public static final BigDecimal DEFAULT_RATE = new BigDecimal("4.50");

    private final BigDecimal defaultRate;
    private final Map<Integer, BigDecimal> rateByYear;

    public TourismTaxPolicy(BigDecimal defaultRate, Map<Integer, BigDecimal> rateByYear) {
        this.defaultRate = defaultRate;
        this.rateByYear = Map.copyOf(rateByYear);
    }

    public BigDecimal ratePerGuestNight(LocalDate checkIn) {
        return rateByYear.getOrDefault(checkIn.getYear(), defaultRate);
    }

    /**
     * @param defaultRate fallback rate, e.g. "4.50" (null = 4.50)
     * @param yearly      per-year overrides such as "2027=4.80,2028=5.00" (may be null)
     */
    public static TourismTaxPolicy parse(String defaultRate, String yearly) {
        Map<Integer, BigDecimal> byYear = new HashMap<>();
        if (yearly != null && !yearly.isBlank()) {
            for (String entry : yearly.split(",")) {
                String[] kv = entry.trim().split("=");
                byYear.put(Integer.parseInt(kv[0].trim()), new BigDecimal(kv[1].trim()));
            }
        }
        BigDecimal base = defaultRate == null || defaultRate.isBlank() ? DEFAULT_RATE : new BigDecimal(defaultRate.trim());
        return new TourismTaxPolicy(base, byYear);
    }
}
