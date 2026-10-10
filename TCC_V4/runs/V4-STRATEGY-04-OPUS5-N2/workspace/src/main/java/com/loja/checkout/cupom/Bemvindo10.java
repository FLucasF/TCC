package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 10% de desconto no valor dos produtos. */
@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal TAXA = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(ContextoCupom pedido) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), TAXA);
    }
}
