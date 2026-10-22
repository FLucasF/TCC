package br.com.pousadamareal.domain;

public enum RatePlan {
    STANDARD("STANDARD", 0.0),
    NON_REFUNDABLE("NON_REFUNDABLE", 0.15);

    private final String value;
    private final double discountRate;

    RatePlan(String value, double discountRate) {
        this.value = value;
        this.discountRate = discountRate;
    }

    public String getValue() {
        return value;
    }

    public double getDiscountRate() {
        return discountRate;
    }
}
