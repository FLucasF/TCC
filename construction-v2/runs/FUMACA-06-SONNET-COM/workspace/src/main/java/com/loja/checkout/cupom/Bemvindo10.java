package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoDesconto contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoDesconto contexto) {
        return Dinheiro.arredondar(contexto.subtotalProdutos().multiply(PERCENTUAL));
    }
}
