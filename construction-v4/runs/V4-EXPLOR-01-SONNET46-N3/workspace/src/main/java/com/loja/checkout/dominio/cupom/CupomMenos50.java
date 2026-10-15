package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.infra.CheckoutException;

import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return DESCONTO;
    }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {
        if (subtotalProdutos.compareTo(MINIMO) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }
}
