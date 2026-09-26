package com.loja.checkout.coupon;

import com.loja.checkout.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50Cupom implements CupomStrategy {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(PedidoContext pedido, BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
        return DESCONTO;
    }
}
