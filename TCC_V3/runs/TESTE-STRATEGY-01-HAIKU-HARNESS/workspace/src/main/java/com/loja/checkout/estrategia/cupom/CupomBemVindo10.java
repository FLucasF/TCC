package com.loja.checkout.estrategia.cupom;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class CupomBemVindo10 implements EstrategiaCupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades) {
        BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
        return Arredondamento.arredondarMeioParaPar(desconto);
    }

    @Override
    public void validar(BigDecimal subtotalProdutos, List<Integer> quantidades) throws IllegalArgumentException {
    }
}
