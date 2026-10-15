package com.loja.checkout.estrategia.clube;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class ClubeOuro implements EstrategiaClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(new BigDecimal("0.05"));
        return Arredondamento.arredondarMeioParaPar(credito);
    }

    @Override
    public BigDecimal getDescontoFrete() {
        return new BigDecimal("1.00");
    }

    @Override
    public boolean verificarBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
