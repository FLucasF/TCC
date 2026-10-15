package com.loja.checkout.domain.regiao;

import com.loja.checkout.domain.Regiao;
import java.math.BigDecimal;

import static com.loja.checkout.util.Arredondamento.arredondar;

public class CentroOeste implements Regiao {

    @Override
    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return arredondar(subtotalProdutos.multiply(new BigDecimal("0.015")));
    }
}
