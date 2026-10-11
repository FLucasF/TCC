package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = BigDecimal.valueOf(0.10);

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Dinheiro.arredondar(contexto.subtotalProdutos().multiply(PERCENTUAL));
    }
}
