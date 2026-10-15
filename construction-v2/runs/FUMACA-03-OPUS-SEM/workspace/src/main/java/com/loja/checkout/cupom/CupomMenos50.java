package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 50,00 de desconto nos produtos, a partir de R$ 300,00 em produtos. */
@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return pedido.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete) {
        return DESCONTO;
    }
}
