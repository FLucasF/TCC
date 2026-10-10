package com.loja.service.estrategia;

import com.loja.domain.NivelClube;
import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class CalculoCredito {
    public static BigDecimal calcular(BigDecimal subtotalProdutos, NivelClube nivel) {
        BigDecimal percentual = switch (nivel) {
            case BRONZE -> new BigDecimal("0.00");
            case PRATA -> new BigDecimal("0.02");
            case OURO -> new BigDecimal("0.05");
        };

        BigDecimal credito = subtotalProdutos.multiply(percentual);
        return Arredondador.arredondarParaCentavos(credito);
    }
}
