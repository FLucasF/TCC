package com.loja.cupom;

import com.loja.domain.Pedido;
import java.math.BigDecimal;

public class Menos50 implements Cupom {
    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return pedido.subtotalProdutos().compareTo(VALOR_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete) {
        return DESCONTO;
    }
}
