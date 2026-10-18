package shop.domain;

import java.math.BigDecimal;

public enum Region {
    SOUTHEAST("1", null, 0),
    SOUTH("1", null, 0),
    MIDWEST("1.5", null, 0),
    NORTH("2.5", "30.00", 3),
    NORTHEAST("2", "30.00", 2);

    private final BigDecimal insurancePercent;
    private final BigDecimal insuranceCap;
    private final int extraCarrierDays;

    Region(String insurancePercent, String insuranceCap, int extraCarrierDays) {
        this.insurancePercent = new BigDecimal(insurancePercent);
        this.insuranceCap = insuranceCap == null ? null : new BigDecimal(insuranceCap);
        this.extraCarrierDays = extraCarrierDays;
    }

    public static Region parse(String value) {
        try {
            return Region.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new DomainException(ErrorCode.INVALID_REGION);
        }
    }

    public BigDecimal insurance(BigDecimal productsSubtotal) {
        BigDecimal value = Money.percent(productsSubtotal, insurancePercent);
        return insuranceCap != null && value.compareTo(insuranceCap) > 0 ? insuranceCap : value;
    }

    /** Dias a mais nas entregas por transportadora, por causa do centro de distribuição. */
    public int extraCarrierDays() {
        return extraCarrierDays;
    }
}
