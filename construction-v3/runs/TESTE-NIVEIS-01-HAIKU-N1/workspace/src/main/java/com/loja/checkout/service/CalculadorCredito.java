package com.loja.checkout.service;

import com.loja.checkout.model.NivelClube;
import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class CalculadorCredito {

    public BigDecimal calcular(BigDecimal subtotalProdutos, NivelClube nivelClube) {
        BigDecimal percentual = switch (nivelClube) {
            case BRONZE -> BigDecimal.ZERO;
            case PRATA -> new BigDecimal("0.02");
            case OURO -> new BigDecimal("0.05");
        };

        BigDecimal credito = subtotalProdutos.multiply(percentual);
        return Arredondador.arredondar(credito);
    }
}
