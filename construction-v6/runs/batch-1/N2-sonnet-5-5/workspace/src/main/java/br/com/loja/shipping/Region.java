package br.com.loja.shipping;

import java.math.BigDecimal;
import java.util.Optional;

import br.com.loja.common.Money;

/** Região do cliente: seguro cobrado pela seguradora e atraso de despacho das transportadoras. */
public enum Region {
    SOUTHEAST("0.01", null, 0),
    SOUTH("0.01", null, 0),
    MIDWEST("0.015", null, 0),
    NORTH("0.025", "30.00", 3),
    NORTHEAST("0.02", "30.00", 2);

    private final String insuranceRate;
    private final BigDecimal insuranceLimit;
    private final int carrierExtraDays;

    Region(String insuranceRate, String insuranceLimit, int carrierExtraDays) {
        this.insuranceRate = insuranceRate;
        this.insuranceLimit = insuranceLimit == null ? null : Money.of(insuranceLimit);
        this.carrierExtraDays = carrierExtraDays;
    }

    public BigDecimal insuranceFor(BigDecimal productsSubtotal) {
        BigDecimal insurance = Money.percent(productsSubtotal, insuranceRate);
        return insuranceLimit != null ? insurance.min(insuranceLimit) : insurance;
    }

    public int carrierExtraDays() {
        return carrierExtraDays;
    }

    public static Optional<Region> parse(String code) {
        for (Region region : values()) {
            if (region.name().equals(code)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }
}
