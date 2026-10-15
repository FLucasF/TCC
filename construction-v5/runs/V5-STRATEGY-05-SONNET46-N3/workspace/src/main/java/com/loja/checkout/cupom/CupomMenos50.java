package com.loja.checkout.cupom;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import com.loja.checkout.model.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return DESCONTO;
    }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {
        if (subtotalProdutos.compareTo(MINIMO) < 0) {
            throw new CheckoutException(ErroCodigo.CUPOM_NAO_APLICAVEL);
        }
    }
}
