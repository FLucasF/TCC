package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Percentual;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CupomBemVindo10 implements Cupom {

    private static final Percentual DESCONTO = Percentual.de("10");

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
        return DESCONTO.sobre(pedido.subtotalProdutos());
    }
}
