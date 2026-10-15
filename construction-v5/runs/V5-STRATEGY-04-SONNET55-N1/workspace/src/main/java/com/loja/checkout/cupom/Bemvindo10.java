package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bemvindo10 implements Cupom {
    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoCupom pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom pedido) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), new BigDecimal("0.10"));
    }
}
