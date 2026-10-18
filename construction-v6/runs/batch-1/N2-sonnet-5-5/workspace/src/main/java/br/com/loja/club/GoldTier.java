package br.com.loja.club;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.common.Money;

@Component
class GoldTier implements ClubTier {

    private static final BigDecimal GIFT_THRESHOLD = Money.of("500.00");

    public String code() {
        return "GOLD";
    }

    public BigDecimal credit(BigDecimal productsSubtotal) {
        return Money.percent(productsSubtotal, "0.05");
    }

    public boolean waivesFreight() {
        return true;
    }

    public boolean gift(BigDecimal productsSubtotal) {
        return productsSubtotal.compareTo(GIFT_THRESHOLD) > 0;
    }
}
