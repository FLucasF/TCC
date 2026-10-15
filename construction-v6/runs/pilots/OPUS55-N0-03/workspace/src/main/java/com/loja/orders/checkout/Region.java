package com.loja.orders.checkout;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import com.loja.orders.common.Money;

/**
 * Regiões do cliente: o seguro cobrado pela seguradora e os dias a mais que o centro de distribuição
 * leva para despachar pelas transportadoras.
 */
public enum Region {
    SOUTHEAST("0.01", null, 0),
    SOUTH("0.01", null, 0),
    MIDWEST("0.015", null, 0),
    NORTH("0.025", "30.00", 3),
    NORTHEAST("0.02", "30.00", 2);

    private final BigDecimal insuranceRate;
    private final BigDecimal insuranceCap;
    private final int carrierDispatchExtraDays;

    Region(String insuranceRate, String insuranceCap, int carrierDispatchExtraDays) {
        this.insuranceRate = new BigDecimal(insuranceRate);
        this.insuranceCap = insuranceCap == null ? null : Money.of(insuranceCap);
        this.carrierDispatchExtraDays = carrierDispatchExtraDays;
    }

    public static Optional<Region> fromCode(String code) {
        return Arrays.stream(values()).filter(r -> r.name().equals(code)).findFirst();
    }

    /** Seguro contra extravio e roubo, sobre o valor dos produtos. */
    public BigDecimal insurance(BigDecimal productsSubtotal) {
        BigDecimal insurance = Money.percentOf(productsSubtotal, insuranceRate);
        return insuranceCap == null ? insurance : Money.min(insurance, insuranceCap);
    }

    public int carrierDispatchExtraDays() {
        return carrierDispatchExtraDays;
    }
}
