package shop.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** Delivery region: insurance pricing and extra dispatch days of the distribution center. */
public enum Region {
    SOUTHEAST("0.01", null, 0),
    SOUTH("0.01", null, 0),
    MIDWEST("0.015", null, 0),
    NORTH("0.025", "30.00", 3),
    NORTHEAST("0.02", "30.00", 2);

    private final BigDecimal insuranceRate;
    private final BigDecimal insuranceCap;
    private final int extraDispatchDays;

    Region(String insuranceRate, String insuranceCap, int extraDispatchDays) {
        this.insuranceRate = new BigDecimal(insuranceRate);
        this.insuranceCap = insuranceCap == null ? null : new BigDecimal(insuranceCap);
        this.extraDispatchDays = extraDispatchDays;
    }

    public BigDecimal insurance(BigDecimal productsSubtotal) {
        BigDecimal value = Money.round(productsSubtotal.multiply(insuranceRate));
        return insuranceCap != null && value.compareTo(insuranceCap) > 0 ? insuranceCap : value;
    }

    public int extraDispatchDays() {
        return extraDispatchDays;
    }

    public static Optional<Region> parse(String code) {
        for (Region r : values()) {
            if (r.name().equals(code)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }
}
