package br.com.marealta;

public enum RatePlan {
    STANDARD(0, true),
    NON_REFUNDABLE(15, false);

    private final int discountPercent;
    private final boolean refundable;

    RatePlan(int discountPercent, boolean refundable) {
        this.discountPercent = discountPercent;
        this.refundable = refundable;
    }

    public int discountPercent() { return discountPercent; }

    public boolean refundable() { return refundable; }
}
