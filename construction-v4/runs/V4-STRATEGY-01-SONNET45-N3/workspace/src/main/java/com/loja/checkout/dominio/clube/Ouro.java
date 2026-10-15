package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.ResultadoClube;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Ouro implements NivelClube {

    @Override
    public ResultadoClube calcular(BigDecimal subtotalProdutos) {
        BigDecimal credito = Arredondamento.arredondar(
            subtotalProdutos.multiply(new BigDecimal("0.05"))
        );
        boolean brinde = subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        return new ResultadoClube(credito, brinde, true);
    }
}
