package shop;

import java.math.BigDecimal;

public enum Region {
    SOUTHEAST("0.01", null, 0),
    SOUTH("0.01", null, 0),
    MIDWEST("0.015", null, 0),
    NORTH("0.025", "30.00", 3),
    NORTHEAST("0.02", "30.00", 2);

    private final String insuranceRate;
    private final BigDecimal insuranceCap;
    private final int carrierExtraDays;

    Region(String insuranceRate, String insuranceCap, int carrierExtraDays) {
        this.insuranceRate = insuranceRate;
        this.insuranceCap = insuranceCap == null ? null : new BigDecimal(insuranceCap);
        this.carrierExtraDays = carrierExtraDays;
    }

    public BigDecimal insurance(BigDecimal productsSubtotal) {
        BigDecimal value = Money.percent(productsSubtotal, insuranceRate);
        return insuranceCap != null && value.compareTo(insuranceCap) > 0 ? Money.round(insuranceCap) : value;
    }

    /** Dias a mais no prazo das opções feitas por transportadora. */
    public int carrierExtraDays() {
        return carrierExtraDays;
    }

    public static Region parse(String code) {
        if (code == null) return null;
        try {
            return valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
