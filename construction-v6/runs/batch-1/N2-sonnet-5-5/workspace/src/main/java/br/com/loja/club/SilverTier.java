package br.com.loja.club;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.common.Money;

@Component
class SilverTier implements ClubTier {

    public String code() {
        return "SILVER";
    }

    public BigDecimal credit(BigDecimal productsSubtotal) {
        return Money.percent(productsSubtotal, "0.02");
    }

    public boolean waivesFreight() {
        return false;
    }

    public boolean gift(BigDecimal productsSubtotal) {
        return false;
    }
}
