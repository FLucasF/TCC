package shop.checkout;

import shop.Money;

import java.math.BigDecimal;

/** Region of the customer: insurance charged by the insurer and extra dispatch days for carrier deliveries. */
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
        return insuranceCap != null && value.compareTo(insuranceCap) > 0 ? Money.round(insuranceCap) : value;
    }

    public int extraDispatchDays() {
        return extraDispatchDays;
    }
}
