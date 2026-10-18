package br.com.loja.pedidos.checkout;

import br.com.loja.pedidos.common.Money;
import java.math.BigDecimal;
import java.util.Optional;

/** Customer region: sets the insurance charge and the extra dispatch days of carrier deliveries. */
public enum Region {
    SOUTHEAST("0.01", null, 0),
    SOUTH("0.01", null, 0),
    MIDWEST("0.015", null, 0),
    NORTH("0.025", "30.00", 3),
    NORTHEAST("0.02", "30.00", 2);

    private final BigDecimal insuranceRate;
    private final BigDecimal insuranceCap;
    private final int dispatchDelayDays;

    Region(String insuranceRate, String insuranceCap, int dispatchDelayDays) {
        this.insuranceRate = new BigDecimal(insuranceRate);
        this.insuranceCap = insuranceCap == null ? null : new BigDecimal(insuranceCap);
        this.dispatchDelayDays = dispatchDelayDays;
    }

    public BigDecimal insurance(BigDecimal productsSubtotal) {
        BigDecimal value = Money.percent(productsSubtotal, insuranceRate);
        return insuranceCap != null && value.compareTo(insuranceCap) > 0 ? insuranceCap : value;
    }

    public int dispatchDelayDays() {
        return dispatchDelayDays;
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
