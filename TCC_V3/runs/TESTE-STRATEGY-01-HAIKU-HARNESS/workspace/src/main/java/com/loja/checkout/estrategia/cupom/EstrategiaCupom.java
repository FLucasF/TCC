package com.loja.checkout.estrategia.cupom;

import java.math.BigDecimal;
import java.util.List;

public interface EstrategiaCupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades);
    void validar(BigDecimal subtotalProdutos, List<Integer> quantidades) throws IllegalArgumentException;
}
