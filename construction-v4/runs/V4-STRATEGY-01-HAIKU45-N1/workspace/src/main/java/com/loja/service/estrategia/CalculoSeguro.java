package com.loja.service.estrategia;

import com.loja.domain.Regiao;
import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class CalculoSeguro {
    public static BigDecimal calcular(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal percentual = switch (regiao) {
            case SUDESTE -> new BigDecimal("0.01");
            case SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };

        BigDecimal seguro = subtotalProdutos.multiply(percentual);
        return Arredondador.arredondarParaCentavos(seguro);
    }
}
