package com.loja.cupom;

import com.loja.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return new BigDecimal("50.00");
    }

    @Override
    public boolean podeAplicar(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
    }
}
