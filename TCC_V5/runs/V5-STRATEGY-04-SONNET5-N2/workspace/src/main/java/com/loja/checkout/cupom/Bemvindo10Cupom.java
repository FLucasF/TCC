package com.loja.checkout.cupom;

import com.loja.checkout.dinheiro.Dinheiro;
import com.loja.checkout.pedido.Pedido;
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
    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.arredondar(pedido.subtotalProdutos().multiply(PERCENTUAL));
    }
}
