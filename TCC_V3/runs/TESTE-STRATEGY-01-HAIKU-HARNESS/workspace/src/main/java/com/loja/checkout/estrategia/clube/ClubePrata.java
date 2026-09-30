package com.loja.checkout.estrategia.clube;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class ClubePrata implements EstrategiaClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(new BigDecimal("0.02"));
        return Arredondamento.arredondarMeioParaPar(credito);
    }

    @Override
    public BigDecimal getDescontoFrete() {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean verificarBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
