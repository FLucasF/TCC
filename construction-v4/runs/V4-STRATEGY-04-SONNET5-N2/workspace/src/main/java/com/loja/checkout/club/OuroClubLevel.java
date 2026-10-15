package com.loja.checkout.club;

import com.loja.checkout.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OuroClubLevel implements ClubLevel {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String getCodigo() {
        return "OURO";
    }

    @Override
    public boolean isFreteGratis() {
        return true;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Money.round(subtotalProdutos.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
