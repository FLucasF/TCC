package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.model.ItemRequest;
import java.util.List;

public class CupomMenos50 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return new BigDecimal("50.00");
    }

    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public boolean ofereceFretGratis() {
        return false;
    }

    @Override
    public String codigo() {
        return "MENOS50";
    }

}
