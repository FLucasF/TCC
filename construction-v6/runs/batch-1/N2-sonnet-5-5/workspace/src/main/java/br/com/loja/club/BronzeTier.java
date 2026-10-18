package br.com.loja.club;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.common.Money;

@Component
class BronzeTier implements ClubTier {

    public String code() {
        return "BRONZE";
    }

    public BigDecimal credit(BigDecimal productsSubtotal) {
        return Money.ZERO;
    }

    public boolean waivesFreight() {
        return false;
    }

    public boolean gift(BigDecimal productsSubtotal) {
        return false;
    }
}
