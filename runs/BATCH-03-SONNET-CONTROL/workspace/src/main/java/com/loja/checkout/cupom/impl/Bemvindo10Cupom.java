package com.loja.checkout.cupom.impl;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.domain.PedidoContexto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10Cupom implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(PedidoContexto pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContexto pedido, BigDecimal frete) {
        return pedido.subtotalProdutos().multiply(PERCENTUAL);
    }
}
