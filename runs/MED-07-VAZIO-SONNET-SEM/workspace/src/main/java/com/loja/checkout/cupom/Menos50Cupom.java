package com.loja.checkout.cupom;

import com.loja.checkout.dominio.PedidoContext;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Menos50Cupom implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal VALOR_MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(PedidoContext pedido) {
        return pedido.subtotalProdutos().compareTo(VALOR_MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal frete) {
        return DESCONTO;
    }
}
