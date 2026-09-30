package com.loja.checkout.estrategia.cupom;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements EstrategiaCupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades) {
        return Arredondamento.arredondarMeioParaPar(new BigDecimal("50.00"));
    }

    @Override
    public void validar(BigDecimal subtotalProdutos, List<Integer> quantidades) throws IllegalArgumentException {
        if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
            throw new IllegalArgumentException("CUPOM_NAO_APLICAVEL");
        }
    }
}
