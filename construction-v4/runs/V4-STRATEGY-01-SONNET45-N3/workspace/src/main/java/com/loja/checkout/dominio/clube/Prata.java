package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.ResultadoClube;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Prata implements NivelClube {

    @Override
    public ResultadoClube calcular(BigDecimal subtotalProdutos) {
        BigDecimal credito = Arredondamento.arredondar(
            subtotalProdutos.multiply(new BigDecimal("0.02"))
        );
        return new ResultadoClube(credito, false, false);
    }
}
