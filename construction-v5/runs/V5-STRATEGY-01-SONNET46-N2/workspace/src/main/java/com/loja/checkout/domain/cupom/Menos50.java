package com.loja.checkout.domain.cupom;

import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CodigoErro;
import com.loja.checkout.web.dto.ItemDto;

import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        if (subtotal.compareTo(MINIMO) < 0) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        return DESCONTO;
    }
}
