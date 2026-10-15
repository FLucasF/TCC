package com.loja.checkout.coupon;

import com.loja.checkout.model.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Menos50 implements Coupon {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
        return subtotalProdutos.compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
        return DESCONTO;
    }
}
