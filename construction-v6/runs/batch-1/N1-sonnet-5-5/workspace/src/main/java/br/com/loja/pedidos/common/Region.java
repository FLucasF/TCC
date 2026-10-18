package br.com.loja.pedidos.common;

import java.math.BigDecimal;

public enum Region {
    SOUTHEAST("0.01", null, 0),
    SOUTH("0.01", null, 0),
    MIDWEST("0.015", null, 0),
    NORTH("0.025", "30.00", 3),
    NORTHEAST("0.02", "30.00", 2);

    private final BigDecimal insuranceRate;
    private final BigDecimal insuranceCap;
    private final int carrierExtraDays;

    Region(String insuranceRate, String insuranceCap, int carrierExtraDays) {
        this.insuranceRate = new BigDecimal(insuranceRate);
        this.insuranceCap = insuranceCap == null ? null : new BigDecimal(insuranceCap);
        this.carrierExtraDays = carrierExtraDays;
    }

    public static Region parse(String code) {
        for (Region region : values()) {
            if (region.name().equals(code)) {
                return region;
            }
        }
        return null;
    }

    public BigDecimal insuranceFor(BigDecimal productsSubtotal) {
        BigDecimal insurance = Money.percent(productsSubtotal, insuranceRate);
        return insuranceCap != null && insurance.compareTo(insuranceCap) > 0 ? Money.round(insuranceCap) : insurance;
    }

    /** Dias a mais de despacho nas entregas feitas por transportadora. */
    public int carrierExtraDays() {
        return carrierExtraDays;
    }
}
