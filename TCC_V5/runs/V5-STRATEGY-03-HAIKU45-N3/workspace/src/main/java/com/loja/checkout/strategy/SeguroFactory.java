package com.loja.checkout.strategy;

import com.loja.checkout.enums.Regiao;
import java.math.BigDecimal;

public class SeguroFactory {

    public static BigDecimal calcularSeguro(Regiao regiao, BigDecimal subtotal) {
        BigDecimal taxa = switch (regiao) {
            case SUDESTE -> new BigDecimal("0.01");
            case SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };

        return subtotal.multiply(taxa);
    }
}
