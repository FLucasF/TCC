package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.ErroCheckout;
import com.loja.checkout.pedido.Item;

@Component
public class Menos50Cupom implements Cupom {

    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");
    private static final BigDecimal VALOR_DESCONTO = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        if (subtotal.compareTo(MINIMO_PRODUTOS) < 0) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return VALOR_DESCONTO;
    }
}
